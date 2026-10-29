package pe.edu.vallegrande.sigrc.medicalhistory.infrastructure.adapter.out.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.exception.UpstreamServiceException;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out.IPatientPort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import io.netty.channel.ChannelOption;

import java.time.Duration;

/**
 * (cambio) Este adaptador ya no crea pacientes en ms-patients: solo lee
 * (existsById, búsquedas por documento, por apellido, por id). Si el
 * paciente no existe, se le pide al usuario registrarlo desde el módulo
 * de Pacientes; ms-medical-history no hace ese registro por él.
 */
@Slf4j
@Component
public class PatientClientAdapter implements IPatientPort {

    private final WebClient webClient;
    private final String    patientsUrl;

    /** Tipos de documento conocidos por ms-patients, en orden de probabilidad de uso. */
    private static final String[] KNOWN_DOCUMENT_TYPES = {"DNI", "CE", "CIE", "PTP", "CPP", "PASSPORT"};

    public PatientClientAdapter(
            WebClient.Builder builder,
            @Value("${services.patients.url:https://lab.vallegrande.edu.pe/sigrc/gateway/api/v1/patients}") String patientsUrl) {

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(30));

        this.webClient = builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        this.patientsUrl = patientsUrl;
    }

    @Override
    public Mono<Boolean> existsById(String patientId) {
        String url = patientsUrl + "/" + patientId;
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    JsonNode data = response.has("data") ? response.get("data") : response;
                    String status = data.has("status") ? data.get("status").asText() : null;
                    return "ACTIVE".equals(status);
                })
                .onErrorResume(err -> {
                    log.warn("No se pudo contactar ms-patients (tolerancia a fallos): {}", describe(err));
                    return Mono.just(true);
                });
    }

    @Override
    public Mono<JsonNode> getPatientById(String patientId) {
        String url = patientsUrl + "/" + patientId;
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> response.has("data") ? response.get("data") : response)
                .onErrorResume(err -> {
                    log.warn("No se pudo obtener paciente {} desde ms-patients: {}", patientId, describe(err));
                    return Mono.just(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode());
                });
    }

    /**
     * Extrae el paciente de la respuesta y descarta el body-fallback del
     * gateway. Cuando ms-patients está caído, el circuit breaker responde
     * HTTP 200 con {"service":"patients","message":...,"status":503}; sin esta
     * validación ese aviso se trataba como un paciente (todos sus campos
     * nulos) y el lookup decía "Paciente encontrado" con datos vacíos.
     */
    private JsonNode unwrapPatientResponse(JsonNode response) {
        if (response != null && response.has("service") && response.path("status").asInt(0) >= 500) {
            throw new UpstreamServiceException(
                    "ms-patients no está disponible: " + response.path("message").asText("sin detalle"), null);
        }
        return response.has("data") ? response.get("data") : response;
    }

    @Override
    public Mono<JsonNode> findByDocumentTypeAndDocumentNumber(String documentType, String documentNumber) {
        String url = patientsUrl + "/document/" + documentType + "/" + documentNumber;
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::unwrapPatientResponse)
                .onErrorResume(err -> {
                    if (err instanceof UpstreamServiceException) {
                        return Mono.error(err);
                    }
                    // (fix) Un 404 real de ms-patients SÍ significa "no existe con ese
                    // documento" y se traduce a Mono.empty() para seguir probando otros
                    // tipos de documento. Cualquier OTRO error (401/403 por token no
                    // propagado, 5xx, timeout, DNS, etc.) NO significa que el paciente no
                    // exista: antes se tragaba igual que un 404 y el usuario terminaba
                    // viendo "Paciente no encontrado" aunque el paciente sí existiera y el
                    // problema fuera de conectividad/autenticación con ms-patients. Ahora
                    // se registra con el status HTTP real y se propaga como error genuino.
                    if (err instanceof WebClientResponseException wcre
                            && wcre.getStatusCode().value() == 404) {
                        log.info("ms-patients: sin coincidencia para {}/{} (404)", documentType, documentNumber);
                        return Mono.empty();
                    }
                    log.error("Fallo real al consultar ms-patients por documento {}/{}: {}",
                            documentType, documentNumber, describe(err));
                    return Mono.error(new UpstreamServiceException(
                            "No se pudo verificar el paciente en ms-patients: " + describe(err), err));
                });
    }

    /**
     * Busca un paciente SOLO por número de documento, sin que el tipo se
     * conozca de antemano. ms-patients no expone (y no se va a tocar) un
     * endpoint "por número sin tipo", así que probamos los tipos conocidos
     * en orden y nos quedamos con el primero que responda 200.
     *
     * (fix) Antes, un fallo real (401/500/timeout) en el primer tipo probado
     * se tragaba y seguía probando los 5 restantes, terminando siempre en
     * "no encontrado" sin dejar rastro del error real. Ahora, si UNO de los
     * intentos falla por un error genuino (no 404), se corta inmediatamente
     * y se propaga ese error en vez de agotar los 6 tipos en falso.
     */
    @Override
    public Mono<JsonNode> findByDocumentNumberOnly(String documentNumber) {
        // concatMap ya avanza al siguiente tipo cuando el Mono de un intento
        // viene vacío (404 real, ver findByDocumentTypeAndDocumentNumber). Si en
        // cambio un intento termina en UpstreamServiceException (error real, no
        // 404), el error se propaga y corta la búsqueda de inmediato en vez de
        // agotar los 6 tipos a ciegas.
        return Flux.fromArray(KNOWN_DOCUMENT_TYPES)
                .concatMap(type -> findByDocumentTypeAndDocumentNumber(type, documentNumber))
                .next();
    }

    @Override
    public Flux<JsonNode> findByLastNameContaining(String lastName) {
        String needle = lastName == null ? "" : lastName.trim().toLowerCase();
        return webClient.get()
                .uri(patientsUrl)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMapMany(response -> {
                    JsonNode data = response.has("data") ? response.get("data") : response;
                    if (data == null || !data.isArray()) {
                        return Flux.empty();
                    }
                    return Flux.fromIterable(data);
                })
                .filter(patient -> {
                    String last = patient.has("lastName") ? patient.get("lastName").asText("") : "";
                    String first = patient.has("firstName") ? patient.get("firstName").asText("") : "";
                    String status = patient.has("status") ? patient.get("status").asText("") : "";
                    boolean matches = last.toLowerCase().contains(needle) || first.toLowerCase().contains(needle);
                    return matches && "ACTIVE".equalsIgnoreCase(status);
                })
                .onErrorResume(err -> {
                    log.warn("No se pudo buscar pacientes por apellido en ms-patients: {}", describe(err));
                    return Flux.empty();
                });
    }

    /**
     * (fix) Antes se hacía PUT /{id} con {"hasHistory": true}: ms-patients
     * ignora ese campo en su actualización general y nunca recibía el número
     * de historia, así que el paciente seguía saliendo "Sin historia". Ahora se
     * usa el PATCH dedicado, que guarda el número y marca hasHistory = true en
     * una sola operación.
     *
     * Si el paciente ya tiene expediente asignado se respeta: el expediente es
     * el de su primera historia y no cambia con cada atención nueva.
     */
    @Override
    public Mono<Void> assignMedicalRecordNumber(String patientId, String historyNumber) {
        if (patientId == null || patientId.isBlank() || historyNumber == null || historyNumber.isBlank()) {
            return Mono.empty();
        }
        return getPatientById(patientId)
                .flatMap(patient -> {
                    String current = patient.path("medicalRecordNumber").asText("");
                    if (!current.isBlank()) {
                        log.info("Paciente {} ya tiene expediente {}; no se reasigna", patientId, current);
                        return Mono.<Void>empty();
                    }
                    return webClient.patch()
                            .uri(patientsUrl + "/" + patientId + "/medical-record-number")
                            .bodyValue(java.util.Map.of("medicalRecordNumber", historyNumber))
                            .retrieve()
                            .bodyToMono(Void.class)
                            .doOnSuccess(v -> log.info("Expediente {} asignado al paciente {}", historyNumber, patientId));
                })
                .onErrorResume(err -> {
                    log.warn("No se pudo asignar el expediente {} en ms-patients para paciente {}: {}",
                            historyNumber, patientId, describe(err));
                    return Mono.empty();
                });
    }

    private String describe(Throwable err) {
        if (err == null) return "error desconocido";
        // (fix) Antes solo se mostraba err.getMessage(), que para
        // WebClientResponseException suele ser genérico ("401 Unauthorized from
        // GET ...") sin distinguirse claramente de un 404. Se antepone el status
        // HTTP explícito para poder diagnosticar de un vistazo en los logs si el
        // fallo es de autenticación (401/403), del propio ms-patients (5xx) o de
        // red/timeout, en vez de asumir siempre "no encontrado".
        if (err instanceof WebClientResponseException wcre) {
            return "HTTP " + wcre.getStatusCode().value() + " " + wcre.getStatusText();
        }
        String msg = err.getMessage();
        if (msg != null && !msg.isBlank()) return msg;
        if (err instanceof io.netty.handler.timeout.ReadTimeoutException) {
            return "tiempo de espera agotado al contactar ms-patients (ReadTimeout, >30s)";
        }
        if (err instanceof io.netty.channel.ConnectTimeoutException) {
            return "no se pudo establecer conexion con ms-patients (ConnectTimeout)";
        }
        return err.getClass().getSimpleName();
    }
}
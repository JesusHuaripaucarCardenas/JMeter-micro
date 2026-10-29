package pe.edu.vallegrande.sigrc.medicalhistory.infrastructure.adapter.out.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.exception.UpstreamServiceException;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.MedicationSnapshot;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out.IMedicationStockPort;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class MedicationStockClientAdapter implements IMedicationStockPort {

    private final WebClient webClient;
    private final String    medicationsUrl;

    public MedicationStockClientAdapter(
            WebClient.Builder builder,
            @Value("${services.medications.url:https://lab.vallegrande.edu.pe/sigrc/gateway/api/v1/medications}") String medicationsUrl) {
        this.webClient      = builder.build();
        this.medicationsUrl = medicationsUrl;
    }

    @Override
    public Mono<MedicationSnapshot> findActiveById(String medicationId) {
        String url = medicationsUrl + "/" + medicationId;
        log.info("Consultando medicamento en stock: {}", url);
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(response -> {
                    JsonNode data = response.has("data") ? response.get("data") : response;
                    if (data == null || data.isNull() || data.isMissingNode()) {
                        return Mono.error(new NotFoundException(
                                "Medicamento no encontrado con ID: " + medicationId));
                    }
                    // ms-medications usa "active"/"inactive" en minúscula, a diferencia de
                    // "ACTIVE"/"INACTIVE" en el resto de microservicios: comparar sin
                    // distinguir mayúsculas evita rechazar medicamentos que sí existen.
                    String status = data.has("status") ? data.get("status").asText() : null;
                    if (!"active".equalsIgnoreCase(status)) {
                        return Mono.error(new NotFoundException(
                                "Medicamento no encontrado o inactivo con ID: " + medicationId));
                    }
                    return Mono.just(MedicationSnapshot.builder()
                            .id(medicationId)
                            .name(text(data, "genericName", "commercialName", "name"))
                            .presentation(text(data, "presentation", "form"))
                            .build());
                })
                .onErrorResume(err -> {
                    if (err instanceof NotFoundException) {
                        return Mono.error(err);
                    }
                    log.error("No se pudo contactar el servicio de stock de medicamentos: {}", err.getMessage());
                    return Mono.error(new UpstreamServiceException(
                            "No se pudo validar el medicamento " + medicationId + " con el servicio de stock", err));
                });
    }

    private String text(JsonNode node, String... candidateFields) {
        for (String field : candidateFields) {
            if (node.has(field) && !node.get(field).isNull()) {
                String value = node.get(field).asText();
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }
}

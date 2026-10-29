package pe.edu.vallegrande.sigrc.medicalhistory.infrastructure.adapter.out.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out.ISpecialtyPort;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class SpecialtyClientAdapter implements ISpecialtyPort {

    private final WebClient webClient;
    private final String    specialtiesUrl;

    public SpecialtyClientAdapter(
            WebClient.Builder builder,
            @Value("${services.specialties.url:https://lab.vallegrande.edu.pe/sigrc/gateway/api/v1/specialties}") String specialtiesUrl) {
        this.webClient      = builder.build();
        this.specialtiesUrl = specialtiesUrl;
    }

    @Override
    public Mono<Boolean> existsById(String specialtyId) {
        String url = specialtiesUrl + "/" + specialtyId;
        log.info("Validando especialidad en: {}", url);
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    JsonNode data = response.has("data") ? response.get("data") : response;
                    if (data == null || data.isNull()) return false;
                    String status = data.has("status") ? data.get("status").asText() : null;
                    return "ACTIVE".equalsIgnoreCase(status);
                })
                .onErrorResume(err -> {
                    log.warn("No se pudo contactar ms-specialties (tolerancia a fallos): {}", err.getMessage());
                    return Mono.just(true);
                });
    }

    @Override
    public Mono<String> getNameById(String specialtyId) {
        if (specialtyId == null || specialtyId.isBlank()) {
            return Mono.just("Desconocida");
        }
        String url = specialtiesUrl + "/" + specialtyId;
        log.info("Obteniendo nombre de especialidad: {}", url);
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    JsonNode data = response.has("data") ? response.get("data") : response;
                    if (data == null || data.isNull() || !data.has("name")) {
                        return "Desconocida";
                    }
                    return data.get("name").asText("Desconocida");
                })
                .onErrorResume(err -> {
                    log.warn("No se pudo obtener nombre de especialidad {}: {}", specialtyId, err.getMessage());
                    return Mono.just("Desconocida");
                });
    }
}

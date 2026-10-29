package pe.edu.vallegrande.sigrc.medicalhistory.infrastructure.adapter.out.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out.IAppointmentPort;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AppointmentClientAdapter implements IAppointmentPort {

    private final WebClient webClient;
    private final String    appointmentsUrl;

    public AppointmentClientAdapter(
            WebClient.Builder builder,
            @Value("${services.appointments.url:https://lab.vallegrande.edu.pe/sigrc/gateway/api/v1/appointments}") String appointmentsUrl) {
        this.webClient       = builder.build();
        this.appointmentsUrl = appointmentsUrl;
    }

    @Override
    public Mono<Boolean> existsById(String appointmentId) {
        String url = appointmentsUrl + "/" + appointmentId;
        log.info("Validando cita en: {}", url);
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    JsonNode data = response.has("data") ? response.get("data") : response;
                    if (data == null || data.isNull()) return false;
                    String status = data.has("status") ? data.get("status").asText() : null;
                    return status != null && !"CANCELADA".equalsIgnoreCase(status);
                })
                .onErrorResume(err -> {
                    log.warn("No se pudo contactar ms-appointments (tolerancia a fallos): {}", err.getMessage());
                    return Mono.just(true);
                });
    }
}

package pe.edu.vallegrande.sigrc.medicalhistory.infrastructure.adapter.out.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.exception.ForbiddenException;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.exception.UpstreamServiceException;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.DoctorProfileSnapshot;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out.IDoctorProfilePort;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out.ISpecialtyPort;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class DoctorProfileClientAdapter implements IDoctorProfilePort {

    private final WebClient webClient;
    private final String    usersUrl;
    private final String    profileUrl;
    private final String    areasUrl;
    private final ISpecialtyPort specialtyPort;

    public DoctorProfileClientAdapter(
            WebClient.Builder builder,
            @Value("${services.users.url:https://lab.vallegrande.edu.pe/sigrc/gateway/api/v1/users}") String usersUrl,
            @Value("${services.profile.url:https://lab.vallegrande.edu.pe/sigrc/gateway/api/v1/profile}") String profileUrl,
            @Value("${services.areas.url:https://lab.vallegrande.edu.pe/sigrc/gateway/api/v1/areas}") String areasUrl,
            ISpecialtyPort specialtyPort) {
        this.webClient = builder.build();
        this.usersUrl = usersUrl;
        this.profileUrl = profileUrl;
        this.areasUrl = areasUrl;
        this.specialtyPort = specialtyPort;
    }

    @Override
    public Mono<DoctorProfileSnapshot> getByUserId(String userId) {
        return webClient.get()
                .uri(profileUrl)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> response.has("data") ? response.get("data") : response)
                .flatMap(this::toSnapshot)
                .onErrorMap(err -> !(err instanceof NotFoundException)
                                && !(err instanceof ForbiddenException)
                                && !(err instanceof UpstreamServiceException),
                        err -> {
                            log.error("No se pudo resolver el perfil del doctor {} en vg-ms-users: {}", userId, err.getMessage());
                            if (err instanceof WebClientResponseException wex) {
                                if (wex.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                                    return new NotFoundException("Perfil del médico autenticado no encontrado en vg-ms-users");
                                }
                                if (wex.getStatusCode().isSameCodeAs(HttpStatus.FORBIDDEN)
                                        || wex.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
                                    return new ForbiddenException("No tiene permisos para consultar el perfil del médico autenticado");
                                }
                            }
                            return new UpstreamServiceException(
                                    "No se pudo verificar el perfil del médico autenticado: " + err.getMessage(), err);
                        });
    }

    private Mono<DoctorProfileSnapshot> toSnapshot(JsonNode user) {
        if (user == null || user.isNull()) {
            return Mono.error(new NotFoundException("Usuario autenticado no encontrado en vg-ms-users"));
        }
        if (user.has("service") && user.path("status").asInt(0) >= 500) {
            return Mono.error(new UpstreamServiceException(
                    "vg-ms-users no está disponible: " + textOrNull(user, "message"), null));
        }

        String role = textOrNull(user, "role");
        boolean isMedical = "DOCTOR".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role);
        if (!isMedical) {
            return Mono.error(new NotFoundException("El usuario autenticado no tiene rol autorizado para atenciones"));
        }
        if (!"ACTIVE".equalsIgnoreCase(textOrNull(user, "status"))) {
            return Mono.error(new NotFoundException("El médico o usuario autenticado está inactivo"));
        }

        DoctorProfileSnapshot.DoctorProfileSnapshotBuilder builder = DoctorProfileSnapshot.builder()
                .firstName(textOrNull(user, "firstName"))
                .lastName(textOrNull(user, "lastName"))
                .motherLastName(textOrNull(user, "motherLastName"))
                .documentType(textOrNull(user, "documentType") != null ? textOrNull(user, "documentType") : "DNI")
                .documentNumber(textOrNull(user, "documentNumber") != null ? textOrNull(user, "documentNumber") : "00000000")
                .phone(textOrNull(user, "phone"));

        JsonNode doctorProfile = user.get("doctorProfile");
        String areaId = doctorProfile != null ? textOrNull(doctorProfile, "areaId") : null;
        if (areaId == null) {
            builder.specialtyId("MED-GEN");
            builder.specialtyName("Medicina General");
            return Mono.just(builder.build());
        }

        return getAreaSpecialtyId(areaId)
                .flatMap(specialtyId -> {
                    if (specialtyId == null) {
                        builder.specialtyId("MED-GEN");
                        builder.specialtyName("Medicina General");
                        return Mono.just(builder.build());
                    }
                    builder.specialtyId(specialtyId);
                    return specialtyPort.getNameById(specialtyId)
                            .map(name -> builder.specialtyName(name).build())
                            .defaultIfEmpty(builder.specialtyName("Medicina General").build());
                })
                .onErrorResume(e -> {
                    builder.specialtyId("MED-GEN");
                    builder.specialtyName("Medicina General");
                    return Mono.just(builder.build());
                });
    }

    private Mono<String> getAreaSpecialtyId(String areaId) {
        return webClient.get()
                .uri(areasUrl + "/" + areaId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    JsonNode data = response.has("data") ? response.get("data") : response;
                    return textOrNull(data, "specialtyId");
                });
    }

    private String textOrNull(JsonNode n, String f) {
        if (n == null || !n.has(f) || n.get(f).isNull()) return null;
        String v = n.get(f).asText();
        return (v == null || v.isBlank()) ? null : v;
    }
}
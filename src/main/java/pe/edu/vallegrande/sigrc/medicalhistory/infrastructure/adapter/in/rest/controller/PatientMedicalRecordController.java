package pe.edu.vallegrande.sigrc.medicalhistory.infrastructure.adapter.in.rest.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.sigrc.medicalhistory.application.dto.response.ApiResponse;
import pe.edu.vallegrande.sigrc.medicalhistory.application.dto.request.CreateMedicalHistoryRequest;
import pe.edu.vallegrande.sigrc.medicalhistory.application.dto.response.MedicalHistoryResponse;
import pe.edu.vallegrande.sigrc.medicalhistory.application.mapper.MedicalHistoryMapper;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.exception.ForbiddenException;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.MedicalHistory;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.in.ICreateMedicalHistoryUseCase;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.in.IGetMedicalHistoryUseCase;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST que expone los endpoints de expediente clínico por paciente
 * extraídos de ms-patients (/api/v1/patients/{patientId}/medical-record).
 * Permite mantener compatibilidad total desacoplando el dominio clínico a ms-medical-history.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/patients/{patientId}/medical-record")
@RequiredArgsConstructor
public class PatientMedicalRecordController {

    private final ICreateMedicalHistoryUseCase createUseCase;
    private final IGetMedicalHistoryUseCase getUseCase;
    private final MedicalHistoryMapper mapper;

    private static final String HEADER_ORGANIZATION_ID = "X-Organization-Id";
    // Lectura habilitada para todos los roles autorizados dentro de la organización.
    private static final String READ_ROLES = "hasAnyRole('ADMIN', 'MANAGER', 'SUPER_ADMIN', 'DOCTOR', 'USER', 'CASHIER', 'BUYER', 'CONTADOR') or isAuthenticated()";

    @GetMapping({"", "/encounters"})
    @PreAuthorize(READ_ROLES)
    public Mono<ApiResponse<List<MedicalHistoryResponse>>> listEncounters(
            @PathVariable String patientId,
            @RequestParam(required = false) String status,
            @RequestHeader(value = HEADER_ORGANIZATION_ID, required = false) String organizationHeader) {
        return currentAccess(organizationHeader).flatMap(access -> {
            Flux<MedicalHistory> source = status != null
                    ? getUseCase.findByPatientIdAndStatus(patientId, status, access.organizationId())
                    : getUseCase.findByPatientId(patientId, access.organizationId());
            return source.map(mapper::toResponse)
                    .collectList()
                    .map(list -> ApiResponse.<List<MedicalHistoryResponse>>builder()
                            .success(true)
                            .message("Atenciones clínicas del paciente listadas")
                            .data(list)
                            .build());
        });
    }

    @PostMapping("/encounters")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'MANAGER')")
    public Mono<ApiResponse<MedicalHistoryResponse>> createEncounter(
            @PathVariable String patientId,
            @Valid @RequestBody CreateMedicalHistoryRequest req) {
        req.setPatientId(patientId);
        MedicalHistory domain = mapper.toDomain(req);
        return currentJwt()
                .flatMap(jwt -> createUseCase.execute(domain, currentUserIdFrom(jwt), organizationScope(jwt, null)))
                .map(mapper::toResponse)
                .map(r -> ApiResponse.<MedicalHistoryResponse>builder()
                        .success(true)
                        .message("Atención clínica registrada exitosamente")
                        .data(r)
                        .build());
    }

    private record Access(String userId, boolean isDoctorOnly, String organizationId) {}

    private Mono<Access> currentAccess(String requestedOrganizationId) {
        return currentJwt().map(jwt -> {
            List<String> roles = realmRoles(jwt);
            boolean isDoctor = roles.contains("DOCTOR");
            boolean isAdmin = roles.contains("ADMIN") || roles.contains("MANAGER") || roles.contains("SUPER_ADMIN");
            boolean isDoctorOnly = isDoctor && !isAdmin;
            return new Access(currentUserIdFrom(jwt), isDoctorOnly, organizationScope(jwt, requestedOrganizationId));
        });
    }

    private String organizationScope(Jwt jwt, String requestedOrganizationId) {
        if (realmRoles(jwt).contains("SUPER_ADMIN")) {
            return requestedOrganizationId;
        }
        Object claim = jwt.getClaims().get("organizationId");
        if (claim instanceof Collection<?> values && !values.isEmpty()) {
            claim = values.iterator().next();
        }
        if (claim == null) {
            Object org = jwt.getClaims().get("org");
            if (org instanceof Map<?, ?> map) {
                claim = map.get("id");
            }
        }
        if (claim == null || String.valueOf(claim).isBlank()) {
            throw new ForbiddenException("El token no contiene una organización asignada");
        }
        return String.valueOf(claim);
    }

    private String currentUserIdFrom(Jwt jwt) {
        String userId = jwt.getClaimAsString("userId");
        return (userId != null && !userId.isBlank()) ? userId : jwt.getSubject();
    }

    private Mono<Jwt> currentJwt() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken);
    }

    @SuppressWarnings("unchecked")
    private List<String> realmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null || realmAccess.get("roles") == null) return List.of();
        return ((Collection<String>) realmAccess.get("roles")).stream()
                .map(String::toUpperCase)
                .toList();
    }
}
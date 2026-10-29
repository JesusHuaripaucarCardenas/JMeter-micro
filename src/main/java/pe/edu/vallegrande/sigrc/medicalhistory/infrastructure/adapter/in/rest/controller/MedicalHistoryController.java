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
import pe.edu.vallegrande.sigrc.medicalhistory.application.dto.request.UpdateMedicalHistoryRequest;
import pe.edu.vallegrande.sigrc.medicalhistory.application.dto.response.MedicalHistoryResponse;
import pe.edu.vallegrande.sigrc.medicalhistory.application.dto.response.PatientLookupResponse;
import pe.edu.vallegrande.sigrc.medicalhistory.application.mapper.MedicalHistoryMapper;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.exception.ForbiddenException;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.MedicalHistory;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.in.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/api/v1/medical-histories", "/api/v1/medical-records/encounters"})
@RequiredArgsConstructor
public class MedicalHistoryController {

    private final ICreateMedicalHistoryUseCase createUseCase;
    private final IGetMedicalHistoryUseCase getUseCase;
    private final IUpdateMedicalHistoryUseCase updateUseCase;
    private final IDeleteMedicalHistoryUseCase deleteUseCase;
    private final IPatientLookupUseCase patientLookupUseCase;
    private final MedicalHistoryMapper mapper;

    private static final String HEADER_ORGANIZATION_ID = "X-Organization-Id";

    // Roles por operación:
    // - Lectura (GET): habilitada para todos los roles autorizados dentro de la organización (médicos, administradores, etc.), permitiendo que cualquier médico consulte el historial completo del paciente.
    // - Creación (POST): médicos y roles administrativos autorizados. Al crear una consulta sobre un paciente existente, queda vinculada al médico autenticado.
    // - Modificación (PUT): EXCLUSIVA para el rol DOCTOR y únicamente el médico autor de la historia.
    // - Desactivar/Restaurar (PATCH): roles administrativos (ADMIN, MANAGER).
    private static final String READ_ROLES   = "hasAnyRole('ADMIN', 'MANAGER', 'SUPER_ADMIN', 'DOCTOR', 'USER', 'CASHIER', 'BUYER', 'CONTADOR') or isAuthenticated()";
    private static final String UPDATE_ROLES = "hasRole('DOCTOR')";
    private static final String ADMIN_ROLES  = "hasAnyRole('ADMIN', 'MANAGER')";

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'MANAGER')")
    public Mono<ApiResponse<MedicalHistoryResponse>> create(
            @Valid @RequestBody CreateMedicalHistoryRequest req) {
        log.info("📥 Recibiendo solicitud de creación: {}", req);
        MedicalHistory domain = mapper.toDomain(req);
        return currentJwt()
                .flatMap(jwt -> createUseCase.execute(domain, currentUserIdFrom(jwt), organizationScope(jwt, null)))
                .map(mapper::toResponse)
                .map(r -> ApiResponse.<MedicalHistoryResponse>builder()
                        .success(true)
                        .message("Historia clínica creada exitosamente")
                        .data(r)
                        .build())
                .doOnSuccess(res -> log.info("✅ Historia creada: {}", res))
                .doOnError(err -> log.error("❌ Error al crear: {}", err.getMessage()));
    }

    @GetMapping
    @PreAuthorize(READ_ROLES)
    public Mono<ApiResponse<List<MedicalHistoryResponse>>> findAll(
            @RequestParam(required = false) String status,
            @RequestHeader(value = HEADER_ORGANIZATION_ID, required = false) String organizationHeader) {
        return currentAccess(organizationHeader).flatMap(access -> {
            Flux<MedicalHistory> source = status != null
                    ? getUseCase.findByStatus(status, access.organizationId())
                    : getUseCase.findAll(access.organizationId());
            return toListResponse(source, "Historias clínicas listadas");
        });
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize(READ_ROLES)
    public Mono<ApiResponse<List<MedicalHistoryResponse>>> findByPatientId(
            @PathVariable String patientId,
            @RequestParam(required = false) String status,
            @RequestHeader(value = HEADER_ORGANIZATION_ID, required = false) String organizationHeader) {
        return currentAccess(organizationHeader).flatMap(access -> {
            Flux<MedicalHistory> source = status != null
                    ? getUseCase.findByPatientIdAndStatus(patientId, status, access.organizationId())
                    : getUseCase.findByPatientId(patientId, access.organizationId());
            return toListResponse(source, "Historias clínicas del paciente");
        });
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize(READ_ROLES)
    public Mono<ApiResponse<List<MedicalHistoryResponse>>> findByDoctorId(
            @PathVariable String doctorId,
            @RequestHeader(value = HEADER_ORGANIZATION_ID, required = false) String organizationHeader) {
        return currentAccess(organizationHeader).flatMap(access -> {
            Flux<MedicalHistory> source = getUseCase.findByDoctorId(doctorId, access.organizationId());
            return toListResponse(source, "Historias clínicas del médico");
        });
    }

    @GetMapping("/appointment/{appointmentId}")
    @PreAuthorize(READ_ROLES)
    public Mono<ApiResponse<List<MedicalHistoryResponse>>> findByAppointmentId(
            @PathVariable String appointmentId,
            @RequestHeader(value = HEADER_ORGANIZATION_ID, required = false) String organizationHeader) {
        return currentAccess(organizationHeader).flatMap(access ->
                toListResponse(getUseCase.findByAppointmentId(appointmentId, access.organizationId()),
                        "Historias clínicas de la cita"));
    }

    @GetMapping("/date-range")
    @PreAuthorize(READ_ROLES)
    public Mono<ApiResponse<List<MedicalHistoryResponse>>> findByDateRange(
            @RequestParam LocalDate start,
            @RequestParam LocalDate end,
            @RequestHeader(value = HEADER_ORGANIZATION_ID, required = false) String organizationHeader) {
        return currentAccess(organizationHeader).flatMap(access ->
                toListResponse(getUseCase.findByVisitDateBetween(start, end, access.organizationId()),
                        "Historias clínicas en el rango de fechas"));
    }

    // ── AUTOCOMPLETADO DE PACIENTE ───────────────────────────────────────────

    /**
     * NUEVO: búsqueda SOLO por número de documento, sin tener que elegir el
     * tipo primero. El backend detecta el tipo real (DNI/CE/CIE/...) y lo
     * devuelve en la respuesta, para que el frontend lo autocomplete también.
     * Es el endpoint que usa el wizard de "Nueva historia clínica".
     */
    @GetMapping("/patient-lookup/document/{documentNumber}")
    @PreAuthorize(READ_ROLES)
    public Mono<ApiResponse<PatientLookupResponse>> lookupPatientByDocumentNumber(
            @PathVariable String documentNumber) {
        return patientLookupUseCase.findByDocumentNumber(documentNumber)
                .map(r -> ApiResponse.<PatientLookupResponse>builder()
                        .success(true)
                        .message("Paciente encontrado")
                        .data(r)
                        .build());
    }

    /** Variante explícita cuando ya se conoce el tipo de documento. */
    @GetMapping("/patient-lookup/document/{documentType}/{documentNumber}")
    @PreAuthorize(READ_ROLES)
    public Mono<ApiResponse<PatientLookupResponse>> lookupPatientByDocument(
            @PathVariable String documentType,
            @PathVariable String documentNumber) {
        return patientLookupUseCase.findByDocument(documentType, documentNumber)
                .map(r -> ApiResponse.<PatientLookupResponse>builder()
                        .success(true)
                        .message("Paciente encontrado")
                        .data(r)
                        .build());
    }

    @GetMapping("/patient-lookup/lastname/{lastName}")
    @PreAuthorize(READ_ROLES)
    public Flux<PatientLookupResponse> lookupPatientsByLastName(@PathVariable String lastName) {
        return patientLookupUseCase.findByLastName(lastName);
    }

    @GetMapping("/patient-lookup/id/{patientId}")
    @PreAuthorize(READ_ROLES)
    public Mono<ApiResponse<PatientLookupResponse>> lookupPatientById(@PathVariable String patientId) {
        return patientLookupUseCase.findByPatientId(patientId)
                .map(r -> ApiResponse.<PatientLookupResponse>builder()
                        .success(true)
                        .message("Paciente encontrado")
                        .data(r)
                        .build());
    }

    // (cambio) Se retiró el endpoint POST /patient-lookup/create: ya no se
    // crean pacientes desde ms-medical-history. Si no existe, se registra
    // desde el módulo de Pacientes (ms-patients) y luego se vuelve a buscar.

    // (cambio) Se retiró el autocompletado de médico ("/doctor-lookup/...):
    // cada doctor solo registra sus propias historias, así que el médico ya
    // no se elige de una lista; el backend lo resuelve del JWT al crear.

    @GetMapping("/{id}")
    @PreAuthorize(READ_ROLES)
    public Mono<ApiResponse<MedicalHistoryResponse>> findById(
            @PathVariable UUID id,
            @RequestHeader(value = HEADER_ORGANIZATION_ID, required = false) String organizationHeader) {
        return currentAccess(organizationHeader).flatMap(access -> getUseCase.findById(id, access.organizationId())
                .map(mapper::toResponse)
                .map(r -> ApiResponse.<MedicalHistoryResponse>builder()
                        .success(true)
                        .message("Historia clínica encontrada")
                        .data(r)
                        .build()));
    }

    @PutMapping("/{id}")
    @PreAuthorize(UPDATE_ROLES)
    public Mono<ApiResponse<MedicalHistoryResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMedicalHistoryRequest req) {
        return currentAccess(null).flatMap(access -> getUseCase.findById(id, access.organizationId())
                .flatMap(mh -> ensureOwned(mh, access))
                .flatMap(mh -> updateUseCase.execute(id, mapper.toDomain(req), access.organizationId()))
                .map(mapper::toResponse)
                .map(r -> ApiResponse.<MedicalHistoryResponse>builder()
                        .success(true)
                        .message("Historia clínica actualizada exitosamente")
                        .data(r)
                        .build()));
    }

    @PatchMapping("/{id}/delete")
    @PreAuthorize(ADMIN_ROLES)
    public Mono<ApiResponse<MedicalHistoryResponse>> deactivate(@PathVariable UUID id) {
        return currentAccess(null)
                .flatMap(access -> deleteUseCase.deactivate(id, access.organizationId()))
                .map(mapper::toResponse)
                .map(r -> ApiResponse.<MedicalHistoryResponse>builder()
                        .success(true)
                        .message("Historia clínica desactivada exitosamente")
                        .data(r)
                        .build());
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize(ADMIN_ROLES)
    public Mono<ApiResponse<MedicalHistoryResponse>> restore(@PathVariable UUID id) {
        return currentAccess(null)
                .flatMap(access -> deleteUseCase.restore(id, access.organizationId()))
                .map(mapper::toResponse)
                .map(r -> ApiResponse.<MedicalHistoryResponse>builder()
                        .success(true)
                        .message("Historia clínica restaurada exitosamente")
                        .data(r)
                        .build());
    }

    // ── Identidad / autorización por dueño ──────────────────────────────────

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

    /**
     * Tenant de la petición. SUPER_ADMIN puede elegir organización con el header
     * X-Organization-Id (o no enviarlo para ver todas); el resto queda atado a la
     * organización de su token y no puede cambiarla.
     */
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

    /** Solo el médico autor puede editar: otros médicos y usuarios pueden consultar la historia clínica del paciente, pero no modificarla. */
    private Mono<MedicalHistory> ensureOwned(MedicalHistory mh, Access access) {
        if (access.userId().equals(mh.getDoctorId())) {
            return Mono.just(mh);
        }
        return Mono.error(new ForbiddenException("Solo el médico que registró la historia clínica puede editarla"));
    }

    private Mono<ApiResponse<List<MedicalHistoryResponse>>> toListResponse(Flux<MedicalHistory> source, String message) {
        return source.map(mapper::toResponse)
                .collectList()
                .map(list -> ApiResponse.<List<MedicalHistoryResponse>>builder()
                        .success(true)
                        .message(message)
                        .data(list)
                        .build());
    }
}

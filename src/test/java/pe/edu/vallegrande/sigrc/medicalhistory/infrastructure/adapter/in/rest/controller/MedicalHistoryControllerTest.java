package pe.edu.vallegrande.sigrc.medicalhistory.infrastructure.adapter.in.rest.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import pe.edu.vallegrande.sigrc.medicalhistory.application.dto.request.CreateMedicalHistoryRequest;
import pe.edu.vallegrande.sigrc.medicalhistory.application.dto.request.UpdateMedicalHistoryRequest;
import pe.edu.vallegrande.sigrc.medicalhistory.application.dto.response.MedicalHistoryResponse;
import pe.edu.vallegrande.sigrc.medicalhistory.application.mapper.MedicalHistoryMapper;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.exception.ForbiddenException;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.MedicalHistory;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.in.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalHistoryControllerTest {

    @Mock private ICreateMedicalHistoryUseCase createUseCase;
    @Mock private IGetMedicalHistoryUseCase getUseCase;
    @Mock private IUpdateMedicalHistoryUseCase updateUseCase;
    @Mock private IDeleteMedicalHistoryUseCase deleteUseCase;
    @Mock private IPatientLookupUseCase patientLookupUseCase;
    @Mock private MedicalHistoryMapper mapper;

    @InjectMocks
    private MedicalHistoryController controller;

    private Jwt adminJwt;
    private JwtAuthenticationToken adminAuth;

    private Jwt doctorJwt;
    private JwtAuthenticationToken doctorAuth;

    @BeforeEach
    void setUp() {
        adminJwt = Jwt.withTokenValue("token-admin")
                .header("alg", "none")
                .subject("admin-1")
                .claim("userId", "admin-1")
                .claim("organizationId", "org-1")
                .claim("realm_access", Map.of("roles", List.of("ADMIN")))
                .build();
        adminAuth = new JwtAuthenticationToken(adminJwt);

        doctorJwt = Jwt.withTokenValue("token-doc")
                .header("alg", "none")
                .subject("doctor-1")
                .claim("userId", "doctor-1")
                .claim("organizationId", "org-1")
                .claim("realm_access", Map.of("roles", List.of("DOCTOR")))
                .build();
        doctorAuth = new JwtAuthenticationToken(doctorJwt);
    }

    @Test
    @DisplayName("findAll para rol administrativo lista todas las historias de la organización sin filtro por especialidad")
    void findAllForAdmin() {
        UUID id = UUID.randomUUID();
        MedicalHistory history = MedicalHistory.builder().id(id).patientId("pat-1").doctorId("doctor-2").organizationId("org-1").build();
        MedicalHistoryResponse response = MedicalHistoryResponse.builder().id(id).patientId("pat-1").doctorId("doctor-2").build();

        when(getUseCase.findAll("org-1")).thenReturn(Flux.just(history));
        when(mapper.toResponse(history)).thenReturn(response);

        StepVerifier.create(controller.findAll(null, null)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(adminAuth)))
                .assertNext(apiResponse -> {
                    assertThat(apiResponse.isSuccess()).isTrue();
                    assertThat(apiResponse.getData()).hasSize(1);
                    assertThat(apiResponse.getData().get(0).getDoctorId()).isEqualTo("doctor-2");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("findAll para médico lista las historias de la organización por estado")
    void findAllForDoctorOnly() {
        UUID id = UUID.randomUUID();
        MedicalHistory history = MedicalHistory.builder().id(id).patientId("pat-1").doctorId("doctor-1").organizationId("org-1").status("ACTIVE").build();
        MedicalHistoryResponse response = MedicalHistoryResponse.builder().id(id).patientId("pat-1").doctorId("doctor-1").build();

        when(getUseCase.findByStatus("ACTIVE", "org-1")).thenReturn(Flux.just(history));
        when(mapper.toResponse(history)).thenReturn(response);

        StepVerifier.create(controller.findAll("ACTIVE", null)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(doctorAuth)))
                .assertNext(apiResponse -> {
                    assertThat(apiResponse.isSuccess()).isTrue();
                    assertThat(apiResponse.getData()).hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("findById para rol administrativo permite consultar cualquier historia del tenant")
    void findByIdForAdmin() {
        UUID id = UUID.randomUUID();
        MedicalHistory history = MedicalHistory.builder().id(id).doctorId("doctor-other").organizationId("org-1").build();
        MedicalHistoryResponse response = MedicalHistoryResponse.builder().id(id).doctorId("doctor-other").build();

        when(getUseCase.findById(id, "org-1")).thenReturn(Mono.just(history));
        when(mapper.toResponse(history)).thenReturn(response);

        StepVerifier.create(controller.findById(id, null)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(adminAuth)))
                .assertNext(apiResponse -> {
                    assertThat(apiResponse.isSuccess()).isTrue();
                    assertThat(apiResponse.getData().getId()).isEqualTo(id);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("findById para médico permite consultar historia clínica de cualquier colega en su organización")
    void findByIdForDoctorOnlyVisible() {
        UUID id = UUID.randomUUID();
        MedicalHistory history = MedicalHistory.builder().id(id).doctorId("doctor-colleague").specialtyId("CARDIO").organizationId("org-1").build();
        MedicalHistoryResponse response = MedicalHistoryResponse.builder().id(id).doctorId("doctor-colleague").build();

        when(getUseCase.findById(id, "org-1")).thenReturn(Mono.just(history));
        when(mapper.toResponse(history)).thenReturn(response);

        StepVerifier.create(controller.findById(id, null)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(doctorAuth)))
                .assertNext(apiResponse -> {
                    assertThat(apiResponse.isSuccess()).isTrue();
                    assertThat(apiResponse.getData().getId()).isEqualTo(id);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("findById propaga NotFound cuando la historia no existe en la organización")
    void findByIdNotFound() {
        UUID id = UUID.randomUUID();

        when(getUseCase.findById(id, "org-1")).thenReturn(Mono.error(new NotFoundException("Historia clínica no encontrada con id: " + id)));

        StepVerifier.create(controller.findById(id, null)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(doctorAuth)))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("update permite modificar únicamente al médico autor de la historia")
    void updateByAuthorDoctor() {
        UUID id = UUID.randomUUID();
        UpdateMedicalHistoryRequest req = new UpdateMedicalHistoryRequest();
        req.setDiagnosis("Nuevo diagnóstico");

        MedicalHistory existing = MedicalHistory.builder().id(id).doctorId("doctor-1").organizationId("org-1").build();
        MedicalHistory updated = MedicalHistory.builder().id(id).doctorId("doctor-1").diagnosis("Nuevo diagnóstico").organizationId("org-1").build();
        MedicalHistoryResponse response = MedicalHistoryResponse.builder().id(id).diagnosis("Nuevo diagnóstico").build();

        when(getUseCase.findById(id, "org-1")).thenReturn(Mono.just(existing));
        when(mapper.toDomain(req)).thenReturn(updated);
        when(updateUseCase.execute(eq(id), any(MedicalHistory.class), eq("org-1"))).thenReturn(Mono.just(updated));
        when(mapper.toResponse(updated)).thenReturn(response);

        StepVerifier.create(controller.update(id, req)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(doctorAuth)))
                .assertNext(apiResponse -> {
                    assertThat(apiResponse.isSuccess()).isTrue();
                    assertThat(apiResponse.getData().getDiagnosis()).isEqualTo("Nuevo diagnóstico");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("update rechaza la modificación cuando otro médico o usuario intenta editar")
    void updateByDifferentDoctorForbidden() {
        UUID id = UUID.randomUUID();
        UpdateMedicalHistoryRequest req = new UpdateMedicalHistoryRequest();

        MedicalHistory existing = MedicalHistory.builder().id(id).doctorId("doctor-author").organizationId("org-1").build();

        when(getUseCase.findById(id, "org-1")).thenReturn(Mono.just(existing));

        StepVerifier.create(controller.update(id, req)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(doctorAuth)))
                .expectError(ForbiddenException.class)
                .verify();
    }
}

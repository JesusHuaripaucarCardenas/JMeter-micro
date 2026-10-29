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
import pe.edu.vallegrande.sigrc.medicalhistory.application.dto.response.MedicalHistoryResponse;
import pe.edu.vallegrande.sigrc.medicalhistory.application.mapper.MedicalHistoryMapper;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.MedicalHistory;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.in.ICreateMedicalHistoryUseCase;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.in.IGetMedicalHistoryUseCase;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientMedicalRecordControllerTest {

    @Mock private ICreateMedicalHistoryUseCase createUseCase;
    @Mock private IGetMedicalHistoryUseCase getUseCase;
    @Mock private MedicalHistoryMapper mapper;

    @InjectMocks
    private PatientMedicalRecordController controller;

    private Jwt jwt;
    private JwtAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("doctor-1")
                .claim("userId", "doctor-1")
                .claim("organizationId", "org-1")
                .claim("realm_access", Map.of("roles", List.of("DOCTOR", "ADMIN")))
                .build();
        authentication = new JwtAuthenticationToken(jwt);
    }

    @Test
    @DisplayName("listEncounters devuelve historias del paciente para rol administrativo")
    void listEncountersSuccess() {
        UUID id = UUID.randomUUID();
        MedicalHistory history = MedicalHistory.builder().id(id).patientId("patient-123").doctorId("doctor-1").organizationId("org-1").build();
        MedicalHistoryResponse response = MedicalHistoryResponse.builder().id(id).patientId("patient-123").doctorId("doctor-1").build();

        when(getUseCase.findByPatientId("patient-123", "org-1")).thenReturn(Flux.just(history));
        when(mapper.toResponse(history)).thenReturn(response);

        StepVerifier.create(controller.listEncounters("patient-123", null, null)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)))
                .assertNext(apiResponse -> {
                    assertThat(apiResponse.isSuccess()).isTrue();
                    assertThat(apiResponse.getData()).hasSize(1);
                    assertThat(apiResponse.getData().get(0).getPatientId()).isEqualTo("patient-123");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("listEncounters devuelve las atenciones del paciente para rol médico dentro de la organización")
    void listEncountersDoctorOnlySuccess() {
        Jwt doctorJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("doctor-1")
                .claim("userId", "doctor-1")
                .claim("organizationId", "org-1")
                .claim("realm_access", Map.of("roles", List.of("DOCTOR")))
                .build();
        JwtAuthenticationToken doctorAuth = new JwtAuthenticationToken(doctorJwt);

        UUID id = UUID.randomUUID();
        MedicalHistory history = MedicalHistory.builder().id(id).patientId("patient-123").doctorId("doctor-colleague").organizationId("org-1").build();
        MedicalHistoryResponse response = MedicalHistoryResponse.builder().id(id).patientId("patient-123").doctorId("doctor-colleague").build();

        when(getUseCase.findByPatientId("patient-123", "org-1")).thenReturn(Flux.just(history));
        when(mapper.toResponse(history)).thenReturn(response);

        StepVerifier.create(controller.listEncounters("patient-123", null, null)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(doctorAuth)))
                .assertNext(apiResponse -> {
                    assertThat(apiResponse.isSuccess()).isTrue();
                    assertThat(apiResponse.getData()).hasSize(1);
                    assertThat(apiResponse.getData().get(0).getPatientId()).isEqualTo("patient-123");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("createEncounter registra la atención asociando el patientId de la URL")
    void createEncounterSuccess() {
        UUID id = UUID.randomUUID();
        CreateMedicalHistoryRequest request = CreateMedicalHistoryRequest.builder().diagnosis("Gripe común").build();
        MedicalHistory domain = MedicalHistory.builder().patientId("patient-123").diagnosis("Gripe común").build();
        MedicalHistory saved = MedicalHistory.builder().id(id).patientId("patient-123").diagnosis("Gripe común").build();
        MedicalHistoryResponse response = MedicalHistoryResponse.builder().id(id).patientId("patient-123").diagnosis("Gripe común").build();

        when(mapper.toDomain(any(CreateMedicalHistoryRequest.class))).thenReturn(domain);
        when(createUseCase.execute(domain, "doctor-1", "org-1")).thenReturn(Mono.just(saved));
        when(mapper.toResponse(saved)).thenReturn(response);

        StepVerifier.create(controller.createEncounter("patient-123", request)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)))
                .assertNext(apiResponse -> {
                    assertThat(apiResponse.isSuccess()).isTrue();
                    assertThat(apiResponse.getData().getId()).isEqualTo(id);
                    assertThat(apiResponse.getData().getPatientId()).isEqualTo("patient-123");
                })
                .verifyComplete();
    }
}
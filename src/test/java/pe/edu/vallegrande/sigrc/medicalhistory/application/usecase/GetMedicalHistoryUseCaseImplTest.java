package pe.edu.vallegrande.sigrc.medicalhistory.application.usecase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.exception.UpstreamServiceException;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.DoctorProfileSnapshot;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.MedicalHistory;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out.IDoctorProfilePort;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out.IMedicalHistoryMedicationRepository;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out.IMedicalHistoryRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMedicalHistoryUseCaseImplTest {

    @Mock private IMedicalHistoryRepository repository;
    @Mock private IMedicalHistoryMedicationRepository medicationRepository;
    @Mock private IDoctorProfilePort doctorProfilePort;

    @InjectMocks
    private GetMedicalHistoryUseCaseImpl useCase;

    private static MedicalHistory history(String doctorId, String specialtyId) {
        return MedicalHistory.builder().doctorId(doctorId).specialtyId(specialtyId).organizationId("org-1").build();
    }

    private void doctorHasSpecialty(String specialtyId) {
        when(doctorProfilePort.getByUserId("doctor-x"))
                .thenReturn(Mono.just(DoctorProfileSnapshot.builder().specialtyId(specialtyId).build()));
    }

    @Test
    @DisplayName("findVisibleToDoctor consulta por el doctor y su especialidad dentro de la organización")
    void findVisibleToDoctorUsesSpecialty() {
        doctorHasSpecialty("PSICO");
        MedicalHistory colleague = history("doctor-y", "PSICO");
        when(repository.findVisibleToDoctor("doctor-x", "PSICO", "org-1")).thenReturn(Flux.just(colleague));

        StepVerifier.create(useCase.findVisibleToDoctor("doctor-x", "org-1"))
                .expectNext(colleague)
                .verifyComplete();
    }

    @Test
    @DisplayName("restrictToDoctor deja pasar las propias y las de su especialidad, no las de otra")
    void restrictToDoctorFiltersBySpecialty() {
        doctorHasSpecialty("PSICO");
        MedicalHistory own = history("doctor-x", "PSICO");
        MedicalHistory colleague = history("doctor-y", "PSICO");
        MedicalHistory otherSpecialty = history("doctor-z", "PEDIA");

        StepVerifier.create(useCase.restrictToDoctor(Flux.just(own, colleague, otherSpecialty), "doctor-x"))
                .expectNext(own, colleague)
                .verifyComplete();
    }

    @Test
    @DisplayName("si no se resuelve la especialidad, el doctor solo ve sus propias historias")
    void restrictToDoctorFallsBackToOwn() {
        when(doctorProfilePort.getByUserId("doctor-x"))
                .thenReturn(Mono.error(new UpstreamServiceException("vg-ms-users caído", null)));
        MedicalHistory own = history("doctor-x", "PSICO");
        MedicalHistory colleague = history("doctor-y", "PSICO");

        StepVerifier.create(useCase.restrictToDoctor(Flux.just(own, colleague), "doctor-x"))
                .expectNext(own)
                .verifyComplete();
    }

    @Test
    @DisplayName("isVisibleToDoctor no consulta el perfil cuando la historia es del propio doctor")
    void isVisibleToDoctorOwnHistory() {
        StepVerifier.create(useCase.isVisibleToDoctor(history("doctor-x", "PEDIA"), "doctor-x"))
                .expectNext(true)
                .verifyComplete();
        verifyNoInteractions(doctorProfilePort);
    }

    @Test
    @DisplayName("isVisibleToDoctor rechaza historias de otra especialidad")
    void isVisibleToDoctorOtherSpecialty() {
        doctorHasSpecialty("PSICO");

        StepVerifier.create(useCase.isVisibleToDoctor(history("doctor-z", "PEDIA"), "doctor-x"))
                .expectNext(false)
                .verifyComplete();
    }
}

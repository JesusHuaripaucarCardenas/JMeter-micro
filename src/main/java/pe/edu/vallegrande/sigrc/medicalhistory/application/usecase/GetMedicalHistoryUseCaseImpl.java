package pe.edu.vallegrande.sigrc.medicalhistory.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.MedicalHistory;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.in.IGetMedicalHistoryUseCase;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out.IDoctorProfilePort;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out.IMedicalHistoryMedicationRepository;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out.IMedicalHistoryRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetMedicalHistoryUseCaseImpl implements IGetMedicalHistoryUseCase {

    private final IMedicalHistoryRepository           repository;
    private final IMedicalHistoryMedicationRepository medicationRepository;
    private final IDoctorProfilePort                  doctorProfilePort;

    @Override
    public Mono<MedicalHistory> findById(UUID id, String organizationId) {
        log.info("Buscando historia clínica con id: {}", id);
        return repository.findById(id, organizationId)
                .switchIfEmpty(Mono.error(new NotFoundException("Historia clínica no encontrada con id: " + id)))
                .flatMap(mh -> medicationRepository.findByMedicalHistoryId(id)
                        .collectList()
                        .map(medications -> {
                            mh.setMedications(medications);
                            return mh;
                        }));
    }

    @Override
    public Flux<MedicalHistory> findAll(String organizationId) {
        log.info("Listando todas las historias clínicas");
        return repository.findAll(organizationId);
    }

    @Override
    public Flux<MedicalHistory> findByStatus(String status, String organizationId) {
        log.info("Listando historias clínicas por estado: {}", status);
        return repository.findByStatus(status, organizationId);
    }

    @Override
    public Flux<MedicalHistory> findByPatientId(String patientId, String organizationId) {
        log.info("Listando historias clínicas del paciente: {}", patientId);
        return repository.findByPatientId(patientId, organizationId);
    }

    @Override
    public Flux<MedicalHistory> findByDoctorId(String doctorId, String organizationId) {
        log.info("Listando historias clínicas del médico: {}", doctorId);
        return repository.findByDoctorId(doctorId, organizationId);
    }

    @Override
    public Flux<MedicalHistory> findByAppointmentId(String appointmentId, String organizationId) {
        log.info("Listando historias clínicas de la cita: {}", appointmentId);
        return repository.findByAppointmentId(appointmentId, organizationId);
    }

    @Override
    public Flux<MedicalHistory> findByVisitDateBetween(LocalDate start, LocalDate end, String organizationId) {
        log.info("Listando historias clínicas entre {} y {}", start, end);
        return repository.findByVisitDateBetween(start, end, organizationId);
    }

    @Override
    public Flux<MedicalHistory> findByPatientIdAndStatus(String patientId, String status, String organizationId) {
        log.info("Listando historias del paciente {} con estado {}", patientId, status);
        return repository.findByPatientIdAndStatus(patientId, status, organizationId);
    }

    // ── Visibilidad del DOCTOR ───────────────────────────────────────────────

    @Override
    public Flux<MedicalHistory> findVisibleToDoctor(String doctorId, String organizationId) {
        log.info("Listando historias visibles para el médico {} (su especialidad, organización {})", doctorId, organizationId);
        return doctorSpecialtyId(doctorId)
                .flatMapMany(specialtyId -> repository.findVisibleToDoctor(doctorId, specialtyId, organizationId));
    }

    @Override
    public Flux<MedicalHistory> restrictToDoctor(Flux<MedicalHistory> source, String doctorId) {
        return doctorSpecialtyId(doctorId)
                .flatMapMany(specialtyId -> source.filter(mh -> isVisible(mh, doctorId, specialtyId)));
    }

    @Override
    public Mono<Boolean> isVisibleToDoctor(MedicalHistory mh, String doctorId) {
        if (doctorId.equals(mh.getDoctorId())) {
            return Mono.just(true);
        }
        return doctorSpecialtyId(doctorId).map(specialtyId -> isVisible(mh, doctorId, specialtyId));
    }

    /** El tenant ya lo acota la consulta; aquí solo se compara dueño o especialidad. */
    private static boolean isVisible(MedicalHistory mh, String doctorId, String specialtyId) {
        return doctorId.equals(mh.getDoctorId())
                || (!specialtyId.isBlank() && specialtyId.equals(mh.getSpecialtyId()));
    }

    /**
     * Especialidad actual del doctor autenticado (derivada de su área en vg-ms-users).
     * Si no se puede resolver, se devuelve vacío y el doctor solo ve sus propias historias.
     */
    private Mono<String> doctorSpecialtyId(String doctorId) {
        return doctorProfilePort.getByUserId(doctorId)
                .map(profile -> profile.getSpecialtyId() != null ? profile.getSpecialtyId() : "")
                .onErrorResume(err -> {
                    log.warn("No se pudo resolver la especialidad del médico {}: {}. Solo verá sus historias.",
                            doctorId, err.getMessage());
                    return Mono.just("");
                })
                .defaultIfEmpty("");
    }
}

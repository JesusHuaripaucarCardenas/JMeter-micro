package pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out;

import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.MedicalHistory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

public interface IMedicalHistoryRepository {
    Mono<MedicalHistory> save(MedicalHistory medicalHistory);
    Mono<Boolean> existsById(UUID id);

    // ── Multi-tenant ─────────────────────────────────────────────────────
    // Todas las lecturas se acotan a la organización. Un organizationId
    // vacío solo lo envía SUPER_ADMIN (solo lectura) y significa "todas".
    Mono<MedicalHistory> findById(UUID id, String organizationId);
    Flux<MedicalHistory> findAll(String organizationId);
    Flux<MedicalHistory> findByStatus(String status, String organizationId);
    Flux<MedicalHistory> findByPatientId(String patientId, String organizationId);
    Flux<MedicalHistory> findByDoctorId(String doctorId, String organizationId);
    Flux<MedicalHistory> findByAppointmentId(String appointmentId, String organizationId);
    Flux<MedicalHistory> findByVisitDateBetween(LocalDate start, LocalDate end, String organizationId);
    Flux<MedicalHistory> findByPatientIdAndStatus(String patientId, String status, String organizationId);
    /** Historias del doctor más las de su especialidad, siempre dentro de la organización. */
    Flux<MedicalHistory> findVisibleToDoctor(String doctorId, String specialtyId, String organizationId);

    /** Próximo correlativo para historyNumber (HC-00000001, HC-00000002, ...). Atómico vía secuencia en BD. */
    Mono<Long> nextHistoryNumberSequence();
}

package pe.edu.vallegrande.sigrc.medicalhistory.domain.port.in;

import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.MedicalHistory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

public interface IGetMedicalHistoryUseCase {
    // organizationId: tenant resuelto del JWT (o del header X-Organization-Id si es SUPER_ADMIN).
    // Vacío = todas las organizaciones (solo SUPER_ADMIN, lectura).
    Mono<MedicalHistory> findById(UUID id, String organizationId);
    Flux<MedicalHistory> findAll(String organizationId);
    Flux<MedicalHistory> findByStatus(String status, String organizationId);
    Flux<MedicalHistory> findByPatientId(String patientId, String organizationId);
    Flux<MedicalHistory> findByDoctorId(String doctorId, String organizationId);
    Flux<MedicalHistory> findByAppointmentId(String appointmentId, String organizationId);
    Flux<MedicalHistory> findByVisitDateBetween(LocalDate start, LocalDate end, String organizationId);
    Flux<MedicalHistory> findByPatientIdAndStatus(String patientId, String status, String organizationId);

    // ── Visibilidad del DOCTOR ───────────────────────────────────────────
    // Un doctor ve sus historias y las de los doctores de su misma
    // especialidad dentro de su organización; nunca las de otra organización.
    Flux<MedicalHistory> findVisibleToDoctor(String doctorId, String organizationId);
    Flux<MedicalHistory> restrictToDoctor(Flux<MedicalHistory> source, String doctorId);
    Mono<Boolean> isVisibleToDoctor(MedicalHistory medicalHistory, String doctorId);
}

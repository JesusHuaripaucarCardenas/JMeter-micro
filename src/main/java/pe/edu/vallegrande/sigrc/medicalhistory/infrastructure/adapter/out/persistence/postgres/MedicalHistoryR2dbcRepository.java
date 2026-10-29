package pe.edu.vallegrande.sigrc.medicalhistory.infrastructure.adapter.out.persistence.postgres;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface MedicalHistoryR2dbcRepository
        extends ReactiveCrudRepository<MedicalHistoryDocument, UUID> {

    Flux<MedicalHistoryDocument> findByStatus(String status);
    Flux<MedicalHistoryDocument> findByPatientId(String patientId);
    Flux<MedicalHistoryDocument> findByDoctorId(String doctorId);
    Flux<MedicalHistoryDocument> findByAppointmentId(String appointmentId);
    Flux<MedicalHistoryDocument> findByPatientIdAndStatus(String patientId, String status);

    // ── Multi-tenant: mismas consultas acotadas a la organización ─────────
    Mono<MedicalHistoryDocument> findByIdAndOrganizationId(UUID id, String organizationId);
    Flux<MedicalHistoryDocument> findByOrganizationId(String organizationId);
    Flux<MedicalHistoryDocument> findByOrganizationIdAndStatus(String organizationId, String status);
    Flux<MedicalHistoryDocument> findByOrganizationIdAndPatientId(String organizationId, String patientId);
    Flux<MedicalHistoryDocument> findByOrganizationIdAndDoctorId(String organizationId, String doctorId);
    Flux<MedicalHistoryDocument> findByOrganizationIdAndAppointmentId(String organizationId, String appointmentId);
    Flux<MedicalHistoryDocument> findByOrganizationIdAndPatientIdAndStatus(String organizationId, String patientId, String status);
}

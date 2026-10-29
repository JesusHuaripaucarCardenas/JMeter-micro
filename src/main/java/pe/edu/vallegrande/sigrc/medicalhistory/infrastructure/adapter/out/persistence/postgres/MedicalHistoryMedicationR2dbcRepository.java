package pe.edu.vallegrande.sigrc.medicalhistory.infrastructure.adapter.out.persistence.postgres;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface MedicalHistoryMedicationR2dbcRepository
        extends ReactiveCrudRepository<MedicalHistoryMedicationDocument, UUID> {

    Flux<MedicalHistoryMedicationDocument> findByMedicalHistoryId(UUID medicalHistoryId);
}

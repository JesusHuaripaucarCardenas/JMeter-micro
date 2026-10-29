package pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out;

import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.MedicalHistoryMedication;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

public interface IMedicalHistoryMedicationRepository {

    Flux<MedicalHistoryMedication> saveAll(UUID medicalHistoryId, List<MedicalHistoryMedication> medications);

    Flux<MedicalHistoryMedication> findByMedicalHistoryId(UUID medicalHistoryId);
}

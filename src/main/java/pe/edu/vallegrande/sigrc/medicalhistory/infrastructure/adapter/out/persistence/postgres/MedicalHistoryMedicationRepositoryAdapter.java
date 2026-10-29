package pe.edu.vallegrande.sigrc.medicalhistory.infrastructure.adapter.out.persistence.postgres;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.MedicalHistoryMedication;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out.IMedicalHistoryMedicationRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MedicalHistoryMedicationRepositoryAdapter implements IMedicalHistoryMedicationRepository {

    private final MedicalHistoryMedicationR2dbcRepository repository;

    @Override
    public Flux<MedicalHistoryMedication> saveAll(UUID medicalHistoryId, List<MedicalHistoryMedication> medications) {
        LocalDateTime now = LocalDateTime.now();
        List<MedicalHistoryMedicationDocument> documents = medications.stream()
                .map(med -> MedicalHistoryMedicationDocument.builder()
                        .medicalHistoryId(medicalHistoryId)
                        .medicationId(med.getMedicationId())
                        .medicationName(med.getMedicationName())
                        .presentation(med.getPresentation())
                        .dose(med.getDose())
                        .frequency(med.getFrequency())
                        .duration(med.getDuration())
                        .quantity(med.getQuantity())
                        .indications(med.getIndications())
                        .status("ACTIVE")
                        .createdAt(now)
                        .updatedAt(now)
                        .build())
                .toList();

        return repository.saveAll(documents).map(this::toDomain);
    }

    @Override
    public Flux<MedicalHistoryMedication> findByMedicalHistoryId(UUID medicalHistoryId) {
        return repository.findByMedicalHistoryId(medicalHistoryId).map(this::toDomain);
    }

    private MedicalHistoryMedication toDomain(MedicalHistoryMedicationDocument doc) {
        return MedicalHistoryMedication.builder()
                .id(doc.getId())
                .medicalHistoryId(doc.getMedicalHistoryId())
                .medicationId(doc.getMedicationId())
                .medicationName(doc.getMedicationName())
                .presentation(doc.getPresentation())
                .dose(doc.getDose())
                .frequency(doc.getFrequency())
                .duration(doc.getDuration())
                .quantity(doc.getQuantity())
                .indications(doc.getIndications())
                .status(doc.getStatus())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}

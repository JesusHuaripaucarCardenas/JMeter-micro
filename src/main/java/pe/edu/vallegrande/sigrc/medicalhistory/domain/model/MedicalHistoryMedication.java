package pe.edu.vallegrande.sigrc.medicalhistory.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalHistoryMedication {

    private UUID id;
    private UUID medicalHistoryId;

    private String medicationId;
    private String medicationName;
    private String presentation;
    private String dose;
    private String frequency;
    private String duration;
    private Integer quantity;
    private String indications;

    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package pe.edu.vallegrande.sigrc.medicalhistory.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionItemResponse {
    private UUID id;
    private String medicationId;
    private String medicationName;
    private String presentation;
    private String dose;
    private String frequency;
    private String duration;
    private Integer quantity;
    private String indications;
    private String status;
}

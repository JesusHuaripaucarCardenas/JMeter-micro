package pe.edu.vallegrande.sigrc.medicalhistory.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionItemRequest {

    @NotBlank(message = "El ID del medicamento es requerido")
    private String medicationId;

    @NotBlank(message = "La dosis es requerida")
    private String dose;

    private String frequency;

    private String duration;

    @NotNull(message = "La cantidad es requerida")
    @Positive(message = "La cantidad debe ser mayor a 0")
    private Integer quantity;

    private String indications;
}

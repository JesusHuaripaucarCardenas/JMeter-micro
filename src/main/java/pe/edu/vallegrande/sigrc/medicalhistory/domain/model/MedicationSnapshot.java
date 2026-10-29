package pe.edu.vallegrande.sigrc.medicalhistory.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Datos del medicamento resueltos desde el microservicio de stock al momento de recetar. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationSnapshot {
    private String id;
    private String name;
    private String presentation;
}

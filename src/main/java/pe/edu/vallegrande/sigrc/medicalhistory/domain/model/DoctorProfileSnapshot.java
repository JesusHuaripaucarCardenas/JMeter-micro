package pe.edu.vallegrande.sigrc.medicalhistory.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Datos del doctor autenticado, resueltos desde vg-ms-users (y su área/especialidad) al crear una historia. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorProfileSnapshot {
    private String firstName;
    private String lastName;
    private String motherLastName;
    private String documentType;
    private String documentNumber;
    private String phone;
    private String specialtyId;
    private String specialtyName;
}

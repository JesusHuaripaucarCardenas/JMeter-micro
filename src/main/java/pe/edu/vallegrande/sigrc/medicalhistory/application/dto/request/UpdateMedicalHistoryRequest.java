package pe.edu.vallegrande.sigrc.medicalhistory.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateMedicalHistoryRequest {
    private String    patientId;
    private String    patientFirstName;
    private String    patientLastName;

    @Pattern(regexp = "DNI|CE|CIE|PTP|CPP|PASSPORT", message = "Tipo de documento inválido")
    private String    patientDocumentType;

    private String    patientDocumentNumber;
    private String    patientGender;
    private LocalDate patientBirthDate;
    private String    patientDistrict;
    private String    patientProvince;
    private String    patientDepartment;
    private String    patientMaritalStatus;
    private String    patientPhone;

    // (cambio) El médico y su especialidad quedan fijados al crear la
    // historia; no son editables desde el cliente.

    private String    appointmentId;
    // (cambio) La fecha/hora de la consulta no es editable; se fijó
    // automáticamente al crear la historia y no cambia al editar.

    // (limpieza) se eliminó el campo suelto "service": duplicaba a specialtyName
    // y nunca se llenaba al crear una historia -> era código muerto.
    @Size(max = 200, message = "El lugar de nacimiento no puede exceder 200 caracteres") private String birthPlace;
    @Size(max = 150, message = "La ocupación no puede exceder 150 caracteres") private String occupation;
    private String diagnosis;
    @Size(max = 20) private String cie10Code;
    private String anamnesis;
    private String background;
    private String medicalEvaluation;
    private String observations;
}

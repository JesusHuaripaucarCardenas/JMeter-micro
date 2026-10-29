package pe.edu.vallegrande.sigrc.medicalhistory.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateMedicalHistoryRequest {

    private String patientId;

    // Nombre y apellido ya no son @NotBlank: cuando se registra una nueva
    // consulta de un paciente que ya tiene historial basta con enviar patientId
    // y el servidor completa los datos desde su última historia. La
    // obligatoriedad se valida en CreateMedicalHistoryUseCaseImpl.
    private String patientFirstName;

    private String patientLastName;

    @Pattern(regexp = "DNI|CE|CIE|PTP|CPP|PASSPORT", message = "Tipo de documento inválido")
    private String patientDocumentType;

    private String patientDocumentNumber;

    @Pattern(regexp = "MASCULINO|FEMENINO", message = "El sexo debe ser MASCULINO o FEMENINO")
    private String patientGender;

    private LocalDate patientBirthDate;

    private String patientDistrict;
    private String patientProvince;
    private String patientDepartment;
    private String patientMaritalStatus;
    private String patientPhone;

    @Size(max = 200, message = "El lugar de nacimiento no puede exceder 200 caracteres")
    private String birthPlace;

    @Size(max = 150)
    private String occupation;

    // (cambio) El médico y su especialidad ya NO se eligen ni se envían desde
    // el cliente: el backend los resuelve del doctor autenticado (JWT) contra
    // vg-ms-users, ya que cada doctor solo puede crear historias propias.

    private String appointmentId;

    // (cambio) La fecha/hora de la consulta ya NO la envía el cliente: se
    // asigna automáticamente en el servidor al momento de crear (ver
    // CreateMedicalHistoryUseCaseImpl), con fecha y hora exactas.

    private String diagnosis;
    @Size(max = 20)
    private String cie10Code;
    private String anamnesis;
    private String background;
    private String medicalEvaluation;
    private String observations;

    /** Receta/recomendación de medicamentos (opcional). */
    @Valid
    private List<PrescriptionItemRequest> medications;
}

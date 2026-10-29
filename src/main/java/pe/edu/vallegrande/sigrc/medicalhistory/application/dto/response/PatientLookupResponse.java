package pe.edu.vallegrande.sigrc.medicalhistory.application.dto.response;

import lombok.*;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PatientLookupResponse {
    private String    patientId;
    private String    firstName;
    private String    lastName;
    private String    fullName;
    private String    documentType;
    private String    documentNumber;
    private String    gender;
    private LocalDate birthDate;
    private Long       age;
    private String    phone;
    private String    maritalStatus;
    private String    district;
    private String    province;
    private String    department;
}

package pe.edu.vallegrande.sigrc.medicalhistory.infrastructure.adapter.out.persistence.postgres;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
@Table("medical_histories")
public class MedicalHistoryDocument {

    @Id
    private UUID id;

    @Column("history_number")
    private String historyNumber;

    @Column("organization_id")
    private String organizationId;

    @Column("patient_id")
    private String patientId;

    @Column("doctor_id")
    private String doctorId;

    @Column("appointment_id")
    private String appointmentId;

    @Column("patient_first_name")
    private String patientFirstName;

    @Column("patient_last_name")
    private String patientLastName;

    @Column("patient_document_type")
    private String patientDocumentType;

    @Column("patient_document_number")
    private String patientDocumentNumber;

    @Column("patient_gender")
    private String patientGender;

    @Column("patient_birth_date")
    private LocalDate patientBirthDate;

    @Column("patient_district")
    private String patientDistrict;

    @Column("patient_province")
    private String patientProvince;

    @Column("patient_department")
    private String patientDepartment;

    @Column("patient_marital_status")
    private String patientMaritalStatus;

    @Column("patient_phone")
    private String patientPhone;

    @Column("doctor_first_name")
    private String doctorFirstName;

    @Column("doctor_last_name")
    private String doctorLastName;

    @Column("doctor_mothers_last_name")
    private String doctorMothersLastName;

    @Column("doctor_document_type")
    private String doctorDocumentType;

    @Column("doctor_document_number")
    private String doctorDocumentNumber;

    @Column("doctor_phone")
    private String doctorPhone;

    @Column("specialty_id")
    private String specialtyId;

    @Column("specialty_name")
    private String specialtyName;

    @Column("visit_date")
    private LocalDateTime visitDate;

    // (limpieza) columna "service" ya no se mapea desde Java: specialty_name
    // es la única fuente de verdad para el "servicio" mostrado en la app.
    // La columna puede seguir existiendo en la BD sin problema (queda NULL).

    @Column("birth_place")
    private String birthPlace;

    @Column("occupation")
    private String occupation;

    @Column("diagnosis")
    private String diagnosis;

    @Column("cie10_code")
    private String cie10Code;

    @Column("anamnesis")
    private String anamnesis;

    @Column("background")
    private String background;

    @Column("medical_evaluation")
    private String medicalEvaluation;

    @Column("observations")
    private String observations;

    @Column("status")
    private String status;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}

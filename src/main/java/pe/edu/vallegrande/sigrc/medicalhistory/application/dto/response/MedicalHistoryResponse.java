package pe.edu.vallegrande.sigrc.medicalhistory.application.dto.response;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalHistoryResponse {

    private UUID id;
    private String historyNumber;
    private String organizationId;

    // ── Referencias ──────────────────────────────────────────────────────
    private String patientId;
    private String doctorId;
    private String appointmentId;

    // ── Datos del paciente ──────────────────────────────────────────────
    private String patientFirstName;
    private String patientLastName;
    private String patientFullName;
    private String patientDocumentType;
    private String patientDocumentNumber;
    private String patientGender;
    private LocalDate patientBirthDate;
    private Long patientAge;
    private String patientPhone;
    private String patientMaritalStatus;
    private String patientDistrict;
    private String patientProvince;
    private String patientDepartment;

    // ── Datos exclusivos ────────────────────────────────────────────────
    private String birthPlace;
    private String occupation;

    // ── Encabezado ──────────────────────────────────────────────────────
    private LocalDateTime visitDate;

    // ── Diagnóstico ─────────────────────────────────────────────────────
    private String diagnosis;
    private String cie10Code;

    // ── Cuerpo clínico ──────────────────────────────────────────────────
    private String anamnesis;
    private String background;
    private String medicalEvaluation;
    private String observations;

    // ── Profesional ─────────────────────────────────────────────────────
    private String doctorFullName;

    // ── Control ─────────────────────────────────────────────────────────
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String doctorFirstName;
    private String doctorLastName;
    private String doctorMothersLastName;
    private String doctorDocumentType;
    private String doctorDocumentNumber;
    private String doctorPhone;
    private String specialtyId;
    private String specialtyName;

    // ── Receta de medicamentos ──────────────────────────────────────────
    private List<PrescriptionItemResponse> medications;
}

package pe.edu.vallegrande.sigrc.medicalhistory.domain.model;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalHistory {

    private UUID id;
    private String historyNumber;

    /** Tenant: id de la organización a la que pertenece el registro (se toma del JWT, nunca del cliente). */
    private String organizationId;

    // ── Referencias externas ──────────────────────────────────────────
    private String patientId;
    /** userId (Mongo _id) del doctor en vg-ms-users; dueño de la historia. */
    private String doctorId;
    private String appointmentId;

    // ── Encabezado de consulta ────────────────────────────────────────
    /** Se asigna automáticamente en el servidor al crear (fecha y hora exactas); no es editable. */
    private LocalDateTime visitDate;
    // (limpieza) se eliminó el campo suelto "service": el servicio mostrado
    // siempre es specialtyName; tener dos campos para lo mismo generaba
    // inconsistencia (nunca se llenaba al crear) y código muerto.

    // ── SNAPSHOT del paciente ─────────────────────────────────────────
    private String    patientFirstName;
    private String    patientLastName;
    private String    patientDocumentType;
    private String    patientDocumentNumber;
    private String    patientGender;
    private LocalDate patientBirthDate;
    private String    patientDistrict;
    private String    patientProvince;
    private String    patientDepartment;
    private String    patientMaritalStatus;
    private String    patientPhone;

    private String birthPlace;
    private String occupation;

    // ── SNAPSHOT del médico ────────────────────────────────────────────
    private String doctorFirstName;
    private String doctorLastName;
    private String doctorMothersLastName;
    private String doctorDocumentType;
    private String doctorDocumentNumber;
    private String doctorPhone;
    private String specialtyId;
    private String specialtyName;

    // ── Receta de medicamentos ──────────────────────────────────────────
    // No se persiste en la fila de medical_histories: vive en la tabla
    // hija medical_history_medications y se ensambla/adjunta en los
    // casos de uso de creación y de lectura por id.
    private List<MedicalHistoryMedication> medications;

    // ── Diagnóstico ────────────────────────────────────────────────────
    private String diagnosis;
    private String cie10Code;

    // ── Cuerpo clínico ─────────────────────────────────────────────────
    private String anamnesis;
    private String background;
    private String medicalEvaluation;
    private String observations;

    // ── Control ────────────────────────────────────────────────────────
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // (limpieza) se eliminó "patientAge": nunca se persistía ni se leía;
    // la edad siempre se calcula al vuelo en MedicalHistoryMapper#toResponse
    // a partir de patientBirthDate, así que el campo era código muerto.
}

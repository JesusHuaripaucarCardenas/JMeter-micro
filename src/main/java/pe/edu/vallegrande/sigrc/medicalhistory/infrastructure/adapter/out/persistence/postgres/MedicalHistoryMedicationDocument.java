package pe.edu.vallegrande.sigrc.medicalhistory.infrastructure.adapter.out.persistence.postgres;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("medical_history_medications")
public class MedicalHistoryMedicationDocument {

    @Id
    private UUID id;

    @Column("medical_history_id")
    private UUID medicalHistoryId;

    @Column("medication_id")
    private String medicationId;

    @Column("medication_name")
    private String medicationName;

    @Column("presentation")
    private String presentation;

    @Column("dose")
    private String dose;

    @Column("frequency")
    private String frequency;

    @Column("duration")
    private String duration;

    @Column("quantity")
    private Integer quantity;

    @Column("indications")
    private String indications;

    @Column("status")
    private String status;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}

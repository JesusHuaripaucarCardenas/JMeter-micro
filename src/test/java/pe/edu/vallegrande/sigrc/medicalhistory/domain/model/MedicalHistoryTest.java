package pe.edu.vallegrande.sigrc.medicalhistory.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MedicalHistoryTest {

    @Test
    @DisplayName("Crear y verificar propiedades del modelo MedicalHistory")
    void testMedicalHistoryBuilderAndProperties() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        LocalDate birthDate = LocalDate.of(1990, 5, 20);

        MedicalHistory history = MedicalHistory.builder()
                .id(id)
                .historyNumber("HC-2026-00001")
                .organizationId("org-test")
                .patientId("pat-123")
                .doctorId("doc-456")
                .appointmentId("apt-789")
                .visitDate(now)
                .patientFirstName("Juan")
                .patientLastName("Perez")
                .patientBirthDate(birthDate)
                .doctorFirstName("Maria")
                .doctorLastName("Lopez")
                .specialtyId("SPEC-01")
                .specialtyName("Medicina General")
                .diagnosis("Diagnostico de prueba")
                .cie10Code("J00")
                .anamnesis("Paciente refiere dolor de cabeza")
                .background("Sin antecedentes de importancia")
                .medicalEvaluation("Examen fisico dentro de limites normales")
                .observations("Sin notas adicionales")
                .status("A")
                .medications(Collections.emptyList())
                .build();

        assertThat(history.getId()).isEqualTo(id);
        assertThat(history.getHistoryNumber()).isEqualTo("HC-2026-00001");
        assertThat(history.getOrganizationId()).isEqualTo("org-test");
        assertThat(history.getPatientId()).isEqualTo("pat-123");
        assertThat(history.getDoctorId()).isEqualTo("doc-456");
        assertThat(history.getAppointmentId()).isEqualTo("apt-789");
        assertThat(history.getVisitDate()).isEqualTo(now);
        assertThat(history.getPatientFirstName()).isEqualTo("Juan");
        assertThat(history.getPatientLastName()).isEqualTo("Perez");
        assertThat(history.getPatientBirthDate()).isEqualTo(birthDate);
        assertThat(history.getDoctorFirstName()).isEqualTo("Maria");
        assertThat(history.getDoctorLastName()).isEqualTo("Lopez");
        assertThat(history.getSpecialtyId()).isEqualTo("SPEC-01");
        assertThat(history.getSpecialtyName()).isEqualTo("Medicina General");
        assertThat(history.getDiagnosis()).isEqualTo("Diagnostico de prueba");
        assertThat(history.getCie10Code()).isEqualTo("J00");
        assertThat(history.getAnamnesis()).isEqualTo("Paciente refiere dolor de cabeza");
        assertThat(history.getBackground()).isEqualTo("Sin antecedentes de importancia");
        assertThat(history.getMedicalEvaluation()).isEqualTo("Examen fisico dentro de limites normales");
        assertThat(history.getObservations()).isEqualTo("Sin notas adicionales");
        assertThat(history.getStatus()).isEqualTo("A");
        assertThat(history.getMedications()).isEmpty();
    }

    @Test
    @DisplayName("Verificar setters y constructor sin argumentos")
    void testNoArgsConstructorAndSetters() {
        MedicalHistory history = new MedicalHistory();
        history.setHistoryNumber("HC-002");
        history.setDoctorId("doc-999");
        history.setStatus("A");

        assertThat(history.getHistoryNumber()).isEqualTo("HC-002");
        assertThat(history.getDoctorId()).isEqualTo("doc-999");
        assertThat(history.getStatus()).isEqualTo("A");
    }
}

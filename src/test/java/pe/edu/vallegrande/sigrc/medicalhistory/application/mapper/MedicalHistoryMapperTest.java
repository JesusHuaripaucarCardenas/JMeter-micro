package pe.edu.vallegrande.sigrc.medicalhistory.application.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.edu.vallegrande.sigrc.medicalhistory.application.dto.response.MedicalHistoryResponse;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.MedicalHistory;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba parametrizada: valida el cálculo de edad del paciente y la
 * construcción del nombre completo en toResponse(MedicalHistory), cubriendo
 * distintos años de nacimiento y combinaciones de nombre del médico
 * (caso de uso: mostrar la historia clínica ya formateada en el front).
 */
class MedicalHistoryMapperTest {

    private final MedicalHistoryMapper mapper = new MedicalHistoryMapper();

    @ParameterizedTest(name = "birthYear={0} -> edad esperada={1}")
    @DisplayName("toResponse calcula la edad del paciente a partir de patientBirthDate")
    @CsvSource({
            "2000, 26",
            "1990, 36",
            "2015, 11"
    })
    void shouldCalculatePatientAge(int birthYear, long expectedMinAge) {
        MedicalHistory history = MedicalHistory.builder()
                .patientFirstName("Ana")
                .patientLastName("Torres")
                .patientBirthDate(LocalDate.of(birthYear, 5, 1))
                .status("ACTIVE")
                .build();

        MedicalHistoryResponse response = mapper.toResponse(history);

        assertThat(response.getPatientAge()).isGreaterThanOrEqualTo(expectedMinAge);
        assertThat(response.getPatientFullName()).isEqualTo("Ana Torres");
    }

    @ParameterizedTest(name = "doctorFirstName={0} lastName={1} mothersLastName={2} -> \"{3}\"")
    @DisplayName("toResponse concatena el nombre completo del médico solo con los datos disponibles")
    @CsvSource(value = {
            "Luis, Ramirez, Soto, 'Luis Ramirez Soto'",
            "Luis, Ramirez, , 'Luis Ramirez'",
            ", Ramirez, , 'Ramirez'"
    })
    void shouldBuildDoctorFullNameFromAvailableFields(String firstName, String lastName,
                                                        String mothersLastName, String expectedFullName) {
        MedicalHistory history = MedicalHistory.builder()
                .patientFirstName("Paciente")
                .patientLastName("Prueba")
                .doctorFirstName(firstName)
                .doctorLastName(lastName)
                .doctorMothersLastName(mothersLastName)
                .status("ACTIVE")
                .build();

        MedicalHistoryResponse response = mapper.toResponse(history);

        assertThat(response.getDoctorFullName()).isEqualTo(expectedFullName);
    }

    @ParameterizedTest(name = "birthDate=null -> edad=null")
    @DisplayName("toResponse deja la edad en null cuando no hay fecha de nacimiento")
    @CsvSource({"true"})
    void shouldReturnNullAgeWhenBirthDateMissing(boolean ignored) {
        MedicalHistory history = MedicalHistory.builder()
                .patientFirstName("Sin")
                .patientLastName("Fecha")
                .status("ACTIVE")
                .build();

        MedicalHistoryResponse response = mapper.toResponse(history);

        assertThat(response.getPatientAge()).isNull();
    }
}

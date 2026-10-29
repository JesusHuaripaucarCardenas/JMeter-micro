package pe.edu.vallegrande.sigrc.medicalhistory.application.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba parametrizada de validación (Bean Validation) de
 * CreateMedicalHistoryRequest: cubre combinaciones de tipo de documento y
 * sexo del paciente (caso de uso: registrar una historia clínica nueva
 * respetando los catálogos permitidos).
 */
class CreateMedicalHistoryRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @ParameterizedTest(name = "documentType=\"{0}\" gender=\"{1}\" -> {2} violaciones")
    @DisplayName("Bean Validation de CreateMedicalHistoryRequest para distintos catálogos")
    @CsvSource({
            "DNI,      MASCULINO, 0",
            "CE,       FEMENINO,  0",
            "PASSPORT, FEMENINO,  0",
            "RUC,      MASCULINO, 1", // tipo de documento fuera de catálogo
            "DNI,      OTRO,      1", // sexo fuera de catálogo
            "RUC,      OTRO,      2"  // ambos fuera de catálogo
    })
    void shouldValidateAccordingToScenario(String documentType, String gender, int expectedViolations) {
        CreateMedicalHistoryRequest request = CreateMedicalHistoryRequest.builder()
                .patientFirstName("Ana")
                .patientLastName("Torres")
                .patientDocumentType(documentType)
                .patientGender(gender)
                .build();

        Set<ConstraintViolation<CreateMedicalHistoryRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(expectedViolations);
    }
}

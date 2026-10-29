package pe.edu.vallegrande.sigrc.medicalhistory.infrastructure.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pe.edu.vallegrande.sigrc.medicalhistory.application.dto.response.ErrorResponse;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.exception.DomainException;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.exception.UpstreamServiceException;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba parametrizada: valida que cada tipo de excepción del microservicio
 * de historias clínicas se traduce al código HTTP correcto, incluyendo el
 * caso propio de este micro (fallo al comunicarse con un servicio externo
 * como ms-doctors o ms-users -> 502 Bad Gateway).
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    static Stream<Arguments> exceptionScenarios() {
        return Stream.of(
                Arguments.of(new NotFoundException("Historia clínica no encontrada"), HttpStatus.NOT_FOUND),
                Arguments.of(new DomainException("Regla de negocio incumplida"), HttpStatus.BAD_REQUEST),
                Arguments.of(new UpstreamServiceException("ms-doctors no responde", new RuntimeException()),
                        HttpStatus.BAD_GATEWAY),
                Arguments.of(new IllegalArgumentException("Argumento inválido"), HttpStatus.BAD_REQUEST),
                Arguments.of(new RuntimeException("Error inesperado"), HttpStatus.INTERNAL_SERVER_ERROR)
        );
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @DisplayName("Cada excepción produce el status HTTP esperado")
    @MethodSource("exceptionScenarios")
    void shouldMapExceptionToHttpStatus(Exception exception, HttpStatus expectedStatus) {
        ResponseEntity<ErrorResponse> response = dispatch(exception);

        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(expectedStatus.value());
    }

    private ResponseEntity<ErrorResponse> dispatch(Exception exception) {
        if (exception instanceof NotFoundException nfe) {
            return handler.handleNotFound(nfe);
        }
        if (exception instanceof UpstreamServiceException use) {
            return handler.handleUpstream(use);
        }
        if (exception instanceof DomainException de) {
            return handler.handleDomain(de);
        }
        if (exception instanceof IllegalArgumentException iae) {
            return handler.handleIllegalArgument(iae);
        }
        return handler.handleGeneric(exception);
    }
}

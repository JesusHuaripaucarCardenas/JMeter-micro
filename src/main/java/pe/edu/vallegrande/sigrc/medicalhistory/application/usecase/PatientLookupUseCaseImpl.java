package pe.edu.vallegrande.sigrc.medicalhistory.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.sigrc.medicalhistory.application.dto.response.PatientLookupResponse;
import pe.edu.vallegrande.sigrc.medicalhistory.application.mapper.MedicalHistoryMapper;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.exception.DomainException;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.exception.UpstreamServiceException;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.in.IPatientLookupUseCase;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out.IPatientPort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientLookupUseCaseImpl implements IPatientLookupUseCase {

    private final IPatientPort         patientPort;
    private final MedicalHistoryMapper mapper;

    @Override
    public Mono<PatientLookupResponse> findByDocument(String documentType, String documentNumber) {
        if (documentType == null || documentType.isBlank()) {
            return Mono.error(new DomainException("El tipo de documento es requerido"));
        }
        if (documentNumber == null || documentNumber.isBlank()) {
            return Mono.error(new DomainException("El número de documento es requerido"));
        }
        log.info("Autocompletado: buscando paciente por documento {}/{}", documentType, documentNumber);
        return patientPort.findByDocumentTypeAndDocumentNumber(documentType, documentNumber)
                .switchIfEmpty(Mono.error(new NotFoundException(
                        "No se encontró paciente con " + documentType + ": " + documentNumber)))
                .map(mapper::toLookupResponse)
                // (fix) mapper::toLookupResponse puede devolver null si el nodo es JSON null;
                // sin esto, el controller arma una respuesta "success" con data=null y el
                // frontend revienta al leer res.data.patientId sobre un data ausente.
                .flatMap(r -> r == null
                        ? Mono.error(new NotFoundException(
                                "No se encontró paciente con " + documentType + ": " + documentNumber))
                        : requirePatientId(r));
    }

    @Override
    public Mono<PatientLookupResponse> findByDocumentNumber(String documentNumber) {
        if (documentNumber == null || documentNumber.isBlank()) {
            return Mono.error(new DomainException("El número de documento es requerido"));
        }
        log.info("Autocompletado: buscando paciente solo por número de documento {}", documentNumber);
        return patientPort.findByDocumentNumberOnly(documentNumber.trim())
                .switchIfEmpty(Mono.error(new NotFoundException(
                        "No se encontró ningún paciente con el documento: " + documentNumber)))
                .map(mapper::toLookupResponse)
                // (fix) idem: si el nodo probado resulta ser JSON null, toLookupResponse
                // devuelve null y switchIfEmpty no lo detecta (el Mono no está vacío,
                // solo contiene null). Lo tratamos explícitamente como "no encontrado".
                .flatMap(r -> r == null
                        ? Mono.error(new NotFoundException(
                                "No se encontró ningún paciente con el documento: " + documentNumber))
                        : requirePatientId(r));
    }

    @Override
    public Flux<PatientLookupResponse> findByLastName(String lastName) {
        if (lastName == null || lastName.isBlank()) {
            return Flux.error(new DomainException("El apellido es requerido para la búsqueda"));
        }
        log.info("Autocompletado: buscando pacientes por apellido '{}'", lastName);
        return patientPort.findByLastNameContaining(lastName)
                .map(mapper::toLookupResponse);
    }

    @Override
    public Mono<PatientLookupResponse> findByPatientId(String patientId) {
        if (patientId == null || patientId.isBlank()) {
            return Mono.error(new DomainException("El ID del paciente es requerido"));
        }
        log.info("Autocompletado: buscando paciente por id {}", patientId);
        return patientPort.getPatientById(patientId)
                .filter(node -> node != null && node.has("firstName"))
                .switchIfEmpty(Mono.error(new NotFoundException("No se encontró paciente con ID: " + patientId)))
                .map(mapper::toLookupResponse);
    }

    /**
     * Un paciente real siempre trae patientId. Si falta, ms-patients no
     * respondió con un paciente válido: se informa como fallo del servicio
     * externo en vez de devolver un "encontrado" con todos los campos nulos.
     */
    private Mono<PatientLookupResponse> requirePatientId(PatientLookupResponse r) {
        if (r.getPatientId() == null || r.getPatientId().isBlank()) {
            return Mono.error(new UpstreamServiceException(
                    "ms-patients devolvió una respuesta sin datos de paciente", null));
        }
        return Mono.just(r);
    }

}

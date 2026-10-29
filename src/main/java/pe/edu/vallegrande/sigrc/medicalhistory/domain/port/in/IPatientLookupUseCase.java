package pe.edu.vallegrande.sigrc.medicalhistory.domain.port.in;

import pe.edu.vallegrande.sigrc.medicalhistory.application.dto.response.PatientLookupResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * (cambio) Ya no expone creación de pacientes: solo búsqueda/lookup. Si el
 * paciente no existe, el frontend debe indicar que se registre en el
 * módulo de Pacientes (ms-patients).
 */
public interface IPatientLookupUseCase {

    Mono<PatientLookupResponse> findByDocument(String documentType, String documentNumber);

    /**
     * NUEVO: busca al paciente solo por número de documento, sin que el
     * usuario tenga que acertar el tipo primero. Recorre los tipos de
     * documento conocidos en ms-patients y, si no hay coincidencia exacta,
     * cae a una búsqueda por número en la lista completa. El tipo de
     * documento real (DNI, CE, etc.) viene resuelto en la respuesta.
     */
    Mono<PatientLookupResponse> findByDocumentNumber(String documentNumber);

    Flux<PatientLookupResponse> findByLastName(String lastName);

    Mono<PatientLookupResponse> findByPatientId(String patientId);
}

package pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida para comunicarse con ms-patients a través de sus endpoints REST.
 * Permite consultar la existencia del paciente, buscar por documento o apellido
 * y asignar el número de expediente tras registrar una atención clínica.
 */
public interface IPatientPort {

    Mono<Boolean> existsById(String patientId);

    Mono<JsonNode> findByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);

    /**
     * Busca un paciente por número de documento SIN conocer el tipo de
     * antemano: permite que en el formulario de Historia Clínica el usuario
     * solo escriba el número y el sistema detecte automáticamente si es
     * DNI, CE, etc.
     */
    Mono<JsonNode> findByDocumentNumberOnly(String documentNumber);

    Flux<JsonNode> findByLastNameContaining(String lastName);

    Mono<JsonNode> getPatientById(String patientId);

    /**
     * Asigna a ms-patients el número de historia como expediente del paciente
     * (PATCH /{id}/medical-record-number), lo que además marca hasHistory = true.
     */
    Mono<Void> assignMedicalRecordNumber(String patientId, String historyNumber);
}
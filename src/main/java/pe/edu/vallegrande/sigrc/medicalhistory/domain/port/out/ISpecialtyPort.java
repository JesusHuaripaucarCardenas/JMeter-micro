package pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out;

import reactor.core.publisher.Mono;

public interface ISpecialtyPort {

    Mono<Boolean> existsById(String specialtyId);

    /** Nombre de la especialidad, "Desconocida" si no se pudo resolver. */
    Mono<String> getNameById(String specialtyId);
}

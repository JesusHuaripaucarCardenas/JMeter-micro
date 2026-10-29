package pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out;

import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.MedicationSnapshot;
import reactor.core.publisher.Mono;

public interface IMedicationStockPort {

    /**
     * Resuelve un medicamento ACTIVE por su ID en el microservicio de stock.
     * A diferencia de los demás ports (paciente/médico/cita/especialidad, que
     * toleran fallos de red devolviendo "true"), este falla explícitamente:
     * NotFoundException si el medicamento no existe/está inactivo,
     * UpstreamServiceException si no se pudo contactar el servicio. Se
     * persiste un snapshot de estos datos en la receta, así que no podemos
     * arriesgarnos a guardar un nombre/presentación incorrectos o vacíos.
     */
    Mono<MedicationSnapshot> findActiveById(String medicationId);
}

package pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out;

import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.DoctorProfileSnapshot;
import reactor.core.publisher.Mono;

/**
 * Resuelve los datos del doctor autenticado contra vg-ms-users (y su área
 * asignada, para el nombre de especialidad). Ya no existe un "vg-ms-doctors"
 * separado: los doctores viven en vg-ms-users y su especialidad se deriva
 * del área que tengan asignada (doctorProfile.areaId -> area.specialtyId).
 */
public interface IDoctorProfilePort {

    /**
     * @param userId id (Mongo _id) del usuario autenticado, tomado del claim
     *                "userId" del JWT.
     * @return snapshot del doctor, o Mono.error(NotFoundException) si el
     *         usuario no existe, no está ACTIVE, no es rol DOCTOR o no tiene
     *         un área (por lo tanto especialidad) asignada.
     */
    Mono<DoctorProfileSnapshot> getByUserId(String userId);
}

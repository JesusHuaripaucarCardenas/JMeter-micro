package pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out;

import reactor.core.publisher.Mono;

public interface IAppointmentPort {
    Mono<Boolean> existsById(String appointmentId);
}

package pe.edu.vallegrande.sigrc.medicalhistory.domain.port.in;

import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.MedicalHistory;
import reactor.core.publisher.Mono;

public interface ICreateMedicalHistoryUseCase {
    /**
     * @param doctorUserId   userId (claim del JWT) del doctor autenticado; dueño y autor de la historia.
     * @param organizationId tenant (claim del JWT) al que queda asociada la historia.
     */
    Mono<MedicalHistory> execute(MedicalHistory medicalHistory, String doctorUserId, String organizationId);
}

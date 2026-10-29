package pe.edu.vallegrande.sigrc.medicalhistory.domain.port.in;

import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.MedicalHistory;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface IDeleteMedicalHistoryUseCase {
    Mono<MedicalHistory> deactivate(UUID id, String organizationId);
    Mono<MedicalHistory> restore(UUID id, String organizationId);
}

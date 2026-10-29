package pe.edu.vallegrande.sigrc.medicalhistory.domain.port.in;

import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.MedicalHistory;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface IUpdateMedicalHistoryUseCase {
    Mono<MedicalHistory> execute(UUID id, MedicalHistory medicalHistory, String organizationId);
}

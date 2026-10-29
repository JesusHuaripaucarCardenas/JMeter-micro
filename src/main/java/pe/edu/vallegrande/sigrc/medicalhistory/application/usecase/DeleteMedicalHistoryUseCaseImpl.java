package pe.edu.vallegrande.sigrc.medicalhistory.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.MedicalHistory;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.in.IDeleteMedicalHistoryUseCase;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out.IMedicalHistoryRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteMedicalHistoryUseCaseImpl implements IDeleteMedicalHistoryUseCase {

    private final IMedicalHistoryRepository repository;

    @Override
    public Mono<MedicalHistory> deactivate(UUID id, String organizationId) {
        log.info("Desactivando historia clínica con id: {}", id);
        return repository.findById(id, organizationId)
                .switchIfEmpty(Mono.error(new NotFoundException("Historia clínica no encontrada con id: " + id)))
                .flatMap(mh -> {
                    mh.setStatus("INACTIVE");
                    return repository.save(mh);
                })
                .doOnSuccess(mh -> log.info("Historia clínica desactivada con id: {}", mh.getId()));
    }

    @Override
    public Mono<MedicalHistory> restore(UUID id, String organizationId) {
        log.info("Restaurando historia clínica con id: {}", id);
        return repository.findById(id, organizationId)
                .switchIfEmpty(Mono.error(new NotFoundException("Historia clínica no encontrada con id: " + id)))
                .flatMap(mh -> {
                    mh.setStatus("ACTIVE");
                    return repository.save(mh);
                })
                .doOnSuccess(mh -> log.info("Historia clínica restaurada con id: {}", mh.getId()));
    }
}

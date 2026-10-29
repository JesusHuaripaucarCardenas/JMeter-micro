package pe.edu.vallegrande.sigrc.medicalhistory.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.exception.DomainException;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.DoctorProfileSnapshot;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.MedicalHistory;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.MedicalHistoryMedication;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.MedicationSnapshot;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.in.ICreateMedicalHistoryUseCase;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out.IAppointmentPort;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out.IDoctorProfilePort;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out.IMedicalHistoryMedicationRepository;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out.IMedicalHistoryRepository;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out.IMedicationStockPort;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.port.out.IPatientPort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateMedicalHistoryUseCaseImpl implements ICreateMedicalHistoryUseCase {

    private final IMedicalHistoryRepository           repository;
    private final IPatientPort                        patientPort;
    private final IDoctorProfilePort                  doctorProfilePort;
    private final IAppointmentPort                    appointmentPort;
    private final IMedicationStockPort                medicationStockPort;
    private final IMedicalHistoryMedicationRepository  medicationRepository;

    private static final String HISTORY_NUMBER_PREFIX = "HC-";
    private static final int    HISTORY_NUMBER_DIGITS  = 8;

    @Override
    public Mono<MedicalHistory> execute(MedicalHistory mh, String doctorUserId, String organizationId) {
        return completePatientData(mh, organizationId)
                .then(Mono.defer(() -> create(mh, doctorUserId, organizationId)));
    }

    /**
     * Nueva consulta de un paciente que ya tiene historial: si el cliente solo
     * envía patientId (sin nombres), los datos del paciente se copian de su
     * historia más reciente en la organización. Si no hay de dónde tomarlos,
     * los nombres siguen siendo obligatorios.
     */
    private Mono<Void> completePatientData(MedicalHistory mh, String organizationId) {
        boolean missingNames = isBlank(mh.getPatientFirstName()) || isBlank(mh.getPatientLastName());
        if (!missingNames) {
            return Mono.empty();
        }
        if (isBlank(mh.getPatientId())) {
            return Mono.error(new DomainException("Los nombres y apellidos del paciente son obligatorios"));
        }
        return repository.findByPatientId(mh.getPatientId(), organizationId)
                .collectList()
                .flatMap(list -> list.stream()
                        .max(Comparator.comparing(MedicalHistory::getVisitDate,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                        .<Mono<MedicalHistory>>map(Mono::just)
                        .orElseGet(() -> Mono.error(new DomainException(
                                "Los nombres y apellidos del paciente son obligatorios"))))
                .doOnNext(last -> copyPatientData(last, mh))
                .then();
    }

    private void copyPatientData(MedicalHistory from, MedicalHistory to) {
        to.setPatientFirstName(firstNonBlank(to.getPatientFirstName(), from.getPatientFirstName()));
        to.setPatientLastName(firstNonBlank(to.getPatientLastName(), from.getPatientLastName()));
        to.setPatientDocumentType(firstNonBlank(to.getPatientDocumentType(), from.getPatientDocumentType()));
        to.setPatientDocumentNumber(firstNonBlank(to.getPatientDocumentNumber(), from.getPatientDocumentNumber()));
        to.setPatientGender(firstNonBlank(to.getPatientGender(), from.getPatientGender()));
        if (to.getPatientBirthDate() == null) to.setPatientBirthDate(from.getPatientBirthDate());
        to.setPatientDistrict(firstNonBlank(to.getPatientDistrict(), from.getPatientDistrict()));
        to.setPatientProvince(firstNonBlank(to.getPatientProvince(), from.getPatientProvince()));
        to.setPatientDepartment(firstNonBlank(to.getPatientDepartment(), from.getPatientDepartment()));
        to.setPatientMaritalStatus(firstNonBlank(to.getPatientMaritalStatus(), from.getPatientMaritalStatus()));
        to.setPatientPhone(firstNonBlank(to.getPatientPhone(), from.getPatientPhone()));
        to.setBirthPlace(firstNonBlank(to.getBirthPlace(), from.getBirthPlace()));
        to.setOccupation(firstNonBlank(to.getOccupation(), from.getOccupation()));
    }

    private static boolean isBlank(String v) {
        return v == null || v.isBlank();
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return isBlank(preferred) ? fallback : preferred;
    }

    private Mono<MedicalHistory> create(MedicalHistory mh, String doctorUserId, String organizationId) {
        log.info("Creando historia clínica para paciente: {} (doctor: {})", mh.getPatientId(), doctorUserId);

        // Si no tiene patientId, se asume que es un registro 100% manual.
        // Si tiene patientId, se valida que exista.
        Mono<Boolean> patientValid = (mh.getPatientId() != null && !mh.getPatientId().isBlank())
                ? patientPort.existsById(mh.getPatientId())
                : Mono.just(true);

        // Validar cita solo si viene appointmentId (es opcional)
        Mono<Boolean> appointmentValid = (mh.getAppointmentId() != null && !mh.getAppointmentId().isBlank())
                ? appointmentPort.existsById(mh.getAppointmentId())
                : Mono.just(true);

        List<MedicalHistoryMedication> requestedMedications =
                mh.getMedications() != null ? mh.getMedications() : List.of();

        // El médico y su especialidad ya no los elige el cliente: se resuelven
        // aquí a partir del doctor autenticado (JWT) contra vg-ms-users.
        return Mono.zip(patientValid, appointmentValid, doctorProfilePort.getByUserId(doctorUserId))
                .flatMap(tuple -> {
                    if (!tuple.getT1()) {
                        return Mono.error(new NotFoundException(
                                "Paciente no encontrado o inactivo con ID: " + mh.getPatientId()));
                    }
                    if (!tuple.getT2()) {
                        return Mono.error(new NotFoundException(
                                "Cita no encontrada o cancelada con ID: " + mh.getAppointmentId()));
                    }
                    DoctorProfileSnapshot doctor = tuple.getT3();

                    mh.setDoctorId(doctorUserId);
                    // Tenant: siempre el del token, nunca lo que mande el cliente.
                    mh.setOrganizationId(organizationId);
                    mh.setDoctorFirstName(doctor.getFirstName());
                    mh.setDoctorLastName(doctor.getLastName());
                    mh.setDoctorMothersLastName(doctor.getMotherLastName());
                    mh.setDoctorDocumentType(doctor.getDocumentType());
                    mh.setDoctorDocumentNumber(doctor.getDocumentNumber());
                    mh.setDoctorPhone(doctor.getPhone());
                    mh.setSpecialtyId(doctor.getSpecialtyId());
                    mh.setSpecialtyName(doctor.getSpecialtyName());
                    mh.setStatus("ACTIVE");
                    // (cambio) Fecha/hora de atención 100% automática del servidor,
                    // con precisión de minutos/segundos; el cliente ya no la envía
                    // ni puede manipularla.
                    mh.setVisitDate(LocalDateTime.now());

                    // ── Resolver cada medicamento contra el stock ANTES de guardar,
                    // así no dejamos una historia creada con una receta a medias si
                    // algún medicamento no existe/está inactivo o el servicio falla.
                    return resolveMedications(requestedMedications)
                            .flatMap(resolved -> repository.nextHistoryNumberSequence()
                                    .map(seq -> {
                                        mh.setHistoryNumber(HISTORY_NUMBER_PREFIX +
                                                String.format("%0" + HISTORY_NUMBER_DIGITS + "d", seq));
                                        return mh;
                                    })
                                    .flatMap(repository::save)
                                    .flatMap(saved -> attachMedications(saved, resolved))
                                    .flatMap(saved -> {
                                        if (saved.getPatientId() != null && !saved.getPatientId().isBlank()) {
                                            return patientPort.assignMedicalRecordNumber(saved.getPatientId(), saved.getHistoryNumber())
                                                    .thenReturn(saved);
                                        }
                                        return Mono.just(saved);
                                    }));
                }).doOnSuccess(saved -> log.info("Historia clínica creada con id: {} y número: {}",
                        saved.getId(), saved.getHistoryNumber()));
    }

    private Mono<List<MedicalHistoryMedication>> resolveMedications(List<MedicalHistoryMedication> requested) {
        if (requested.isEmpty()) {
            return Mono.just(List.of());
        }
        return Flux.fromIterable(requested)
                .concatMap(item -> medicationStockPort.findActiveById(item.getMedicationId())
                        .map(snapshot -> enrich(item, snapshot)))
                .collectList();
    }

    private MedicalHistoryMedication enrich(MedicalHistoryMedication item, MedicationSnapshot snapshot) {
        item.setMedicationName(snapshot.getName());
        item.setPresentation(snapshot.getPresentation());
        return item;
    }

    private Mono<MedicalHistory> attachMedications(MedicalHistory saved, List<MedicalHistoryMedication> resolved) {
        if (resolved.isEmpty()) {
            saved.setMedications(List.of());
            return Mono.just(saved);
        }
        return medicationRepository.saveAll(saved.getId(), resolved)
                .collectList()
                .map(persisted -> {
                    saved.setMedications(persisted);
                    return saved;
                });
    }
}

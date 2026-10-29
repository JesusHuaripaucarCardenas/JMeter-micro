package pe.edu.vallegrande.sigrc.medicalhistory.application.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.sigrc.medicalhistory.application.dto.request.*;
import pe.edu.vallegrande.sigrc.medicalhistory.application.dto.response.MedicalHistoryResponse;
import pe.edu.vallegrande.sigrc.medicalhistory.application.dto.response.PatientLookupResponse;
import pe.edu.vallegrande.sigrc.medicalhistory.application.dto.response.PrescriptionItemResponse;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.MedicalHistory;
import pe.edu.vallegrande.sigrc.medicalhistory.domain.model.MedicalHistoryMedication;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class MedicalHistoryMapper {

    public MedicalHistory toDomain(CreateMedicalHistoryRequest req) {
        return MedicalHistory.builder()
                .patientId(req.getPatientId())
                .patientFirstName(req.getPatientFirstName())
                .patientLastName(req.getPatientLastName())
                .patientDocumentType(req.getPatientDocumentType())
                .patientDocumentNumber(req.getPatientDocumentNumber())
                .patientGender(req.getPatientGender())
                .patientBirthDate(req.getPatientBirthDate())
                .patientDistrict(req.getPatientDistrict())
                .patientProvince(req.getPatientProvince())
                .patientDepartment(req.getPatientDepartment())
                .patientMaritalStatus(req.getPatientMaritalStatus())
                .patientPhone(req.getPatientPhone())
                .birthPlace(req.getBirthPlace())
                .occupation(req.getOccupation())
                .appointmentId(req.getAppointmentId())
                .diagnosis(req.getDiagnosis())
                .cie10Code(req.getCie10Code())
                .anamnesis(req.getAnamnesis())
                .background(req.getBackground())
                .medicalEvaluation(req.getMedicalEvaluation())
                .observations(req.getObservations())
                .status("ACTIVE")
                .medications(toMedications(req.getMedications()))
                .build();
    }

    private List<MedicalHistoryMedication> toMedications(List<PrescriptionItemRequest> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> MedicalHistoryMedication.builder()
                        .medicationId(item.getMedicationId())
                        .dose(item.getDose())
                        .frequency(item.getFrequency())
                        .duration(item.getDuration())
                        .quantity(item.getQuantity())
                        .indications(item.getIndications())
                        .build())
                .toList();
    }

    public MedicalHistory toDomain(UpdateMedicalHistoryRequest req) {
        return MedicalHistory.builder()
                .patientId(req.getPatientId())
                .patientFirstName(req.getPatientFirstName())
                .patientLastName(req.getPatientLastName())
                .patientDocumentType(req.getPatientDocumentType())
                .patientDocumentNumber(req.getPatientDocumentNumber())
                .patientGender(req.getPatientGender())
                .patientBirthDate(req.getPatientBirthDate())
                .patientDistrict(req.getPatientDistrict())
                .patientProvince(req.getPatientProvince())
                .patientDepartment(req.getPatientDepartment())
                .patientMaritalStatus(req.getPatientMaritalStatus())
                .patientPhone(req.getPatientPhone())
                .birthPlace(req.getBirthPlace())
                .occupation(req.getOccupation())
                .appointmentId(req.getAppointmentId())
                .diagnosis(req.getDiagnosis())
                .cie10Code(req.getCie10Code())
                .anamnesis(req.getAnamnesis())
                .background(req.getBackground())
                .medicalEvaluation(req.getMedicalEvaluation())
                .observations(req.getObservations())
                .build();
    }

    public MedicalHistoryResponse toResponse(MedicalHistory mh) {
        Long age = mh.getPatientBirthDate() != null
                ? ChronoUnit.YEARS.between(mh.getPatientBirthDate(), LocalDate.now())
                : null;

        String doctorFullName = "";
        if (mh.getDoctorFirstName() != null) doctorFullName += mh.getDoctorFirstName() + " ";
        if (mh.getDoctorLastName() != null) doctorFullName += mh.getDoctorLastName() + " ";
        if (mh.getDoctorMothersLastName() != null) doctorFullName += mh.getDoctorMothersLastName();
        doctorFullName = doctorFullName.trim();

        String patientFullName = ((mh.getPatientFirstName() != null ? mh.getPatientFirstName() : "") + " "
                + (mh.getPatientLastName() != null ? mh.getPatientLastName() : "")).trim();

        return MedicalHistoryResponse.builder()
                .id(mh.getId())
                .historyNumber(mh.getHistoryNumber())
                .organizationId(mh.getOrganizationId())
                .patientId(mh.getPatientId())
                .patientFirstName(mh.getPatientFirstName())
                .patientLastName(mh.getPatientLastName())
                .patientFullName(patientFullName)
                .patientDocumentType(mh.getPatientDocumentType())
                .patientDocumentNumber(mh.getPatientDocumentNumber())
                .patientGender(mh.getPatientGender())
                .patientBirthDate(mh.getPatientBirthDate())
                .patientAge(age)
                .patientPhone(mh.getPatientPhone())
                .patientMaritalStatus(mh.getPatientMaritalStatus())
                .patientDistrict(mh.getPatientDistrict())
                .patientProvince(mh.getPatientProvince())
                .patientDepartment(mh.getPatientDepartment())
                .birthPlace(mh.getBirthPlace())
                .occupation(mh.getOccupation())
                .doctorId(mh.getDoctorId())
                .doctorFirstName(mh.getDoctorFirstName())
                .doctorLastName(mh.getDoctorLastName())
                .doctorMothersLastName(mh.getDoctorMothersLastName())
                .doctorFullName(doctorFullName)
                .doctorDocumentType(mh.getDoctorDocumentType())
                .doctorDocumentNumber(mh.getDoctorDocumentNumber())
                .doctorPhone(mh.getDoctorPhone())
                .specialtyId(mh.getSpecialtyId())
                .specialtyName(mh.getSpecialtyName())
                .medications(toPrescriptionResponses(mh.getMedications()))
                .appointmentId(mh.getAppointmentId())
                .visitDate(mh.getVisitDate())
                .diagnosis(mh.getDiagnosis())
                .cie10Code(mh.getCie10Code())
                .anamnesis(mh.getAnamnesis())
                .background(mh.getBackground())
                .medicalEvaluation(mh.getMedicalEvaluation())
                .observations(mh.getObservations())
                .status(mh.getStatus())
                .createdAt(mh.getCreatedAt())
                .updatedAt(mh.getUpdatedAt())
                .build();
    }

    private List<PrescriptionItemResponse> toPrescriptionResponses(List<MedicalHistoryMedication> medications) {
        if (medications == null) {
            return List.of();
        }
        return medications.stream()
                .map(med -> PrescriptionItemResponse.builder()
                        .id(med.getId())
                        .medicationId(med.getMedicationId())
                        .medicationName(med.getMedicationName())
                        .presentation(med.getPresentation())
                        .dose(med.getDose())
                        .frequency(med.getFrequency())
                        .duration(med.getDuration())
                        .quantity(med.getQuantity())
                        .indications(med.getIndications())
                        .status(med.getStatus())
                        .build())
                .toList();
    }

    public PatientLookupResponse toLookupResponse(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }

        LocalDate birthDate = node.has("birthDate") && !node.get("birthDate").isNull()
                ? LocalDate.parse(node.get("birthDate").asText())
                : null;
        Long age = birthDate != null ? ChronoUnit.YEARS.between(birthDate, LocalDate.now()) : null;

        JsonNode address = node.get("address");
        String firstName = textOrNull(node, "firstName");
        String lastName = textOrNull(node, "lastName");

        return PatientLookupResponse.builder()
                .patientId(textOrNull(node, "patientId"))
                .firstName(firstName)
                .lastName(lastName)
                .fullName(((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim())
                .documentType(textOrNull(node, "documentType"))
                .documentNumber(textOrNull(node, "documentNumber"))
                .gender(textOrNull(node, "gender"))
                .birthDate(birthDate)
                .age(age)
                .phone(textOrNull(node, "phone"))
                .maritalStatus(textOrNull(node, "maritalStatus"))
                .district(address != null ? textOrNull(address, "adminLevel3") : null)
                .province(address != null ? textOrNull(address, "adminLevel2") : null)
                .department(address != null ? textOrNull(address, "adminLevel1") : null)
                .build();
    }

    private String textOrNull(JsonNode n, String f) {
        if (n == null || !n.has(f) || n.get(f).isNull()) return null;
        String v = n.get(f).asText();
        return (v == null || v.isBlank()) ? null : v;
    }
}

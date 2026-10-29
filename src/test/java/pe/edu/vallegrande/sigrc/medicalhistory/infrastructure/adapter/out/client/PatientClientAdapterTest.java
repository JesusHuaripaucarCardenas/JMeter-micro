package pe.edu.vallegrande.sigrc.medicalhistory.infrastructure.adapter.out.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sin servidor real: el ExchangeFunction del builder responde según el método
 * y registra cada petición para poder comprobar qué se le mandó a ms-patients.
 */
class PatientClientAdapterTest {

    private static final String URL = "http://patients/api/v1/patients";

    private final List<ClientRequest> requests = new ArrayList<>();

    private PatientClientAdapter adapter(String patientJson, HttpStatus patchStatus) {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
            requests.add(request);
            if (request.method() == HttpMethod.GET) {
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(patientJson)
                        .build());
            }
            return Mono.just(ClientResponse.create(patchStatus).build());
        });
        return new PatientClientAdapter(builder, URL);
    }

    @Test
    @DisplayName("Paciente sin expediente: se le asigna el número de historia por PATCH")
    void assignsHistoryNumberWhenPatientHasNone() {
        PatientClientAdapter adapter = adapter("{\"data\":{\"id\":\"p-1\",\"medicalRecordNumber\":null}}", HttpStatus.OK);

        StepVerifier.create(adapter.assignMedicalRecordNumber("p-1", "HC-00000001")).verifyComplete();

        assertThat(requests).hasSize(2);
        ClientRequest patch = requests.get(1);
        assertThat(patch.method()).isEqualTo(HttpMethod.PATCH);
        assertThat(patch.url()).hasToString(URL + "/p-1/medical-record-number");
    }

    @Test
    @DisplayName("Paciente con expediente: no se reasigna")
    void keepsExistingMedicalRecordNumber() {
        PatientClientAdapter adapter = adapter("{\"data\":{\"id\":\"p-1\",\"medicalRecordNumber\":\"HC-00000001\"}}", HttpStatus.OK);

        StepVerifier.create(adapter.assignMedicalRecordNumber("p-1", "HC-00000007")).verifyComplete();

        assertThat(requests).extracting(ClientRequest::method).containsExactly(HttpMethod.GET);
    }

    @Test
    @DisplayName("Si ms-patients falla en el PATCH, no se propaga el error")
    void toleratesPatchFailure() {
        PatientClientAdapter adapter = adapter("{\"data\":{\"id\":\"p-1\"}}", HttpStatus.INTERNAL_SERVER_ERROR);

        StepVerifier.create(adapter.assignMedicalRecordNumber("p-1", "HC-00000001")).verifyComplete();

        assertThat(requests).hasSize(2);
    }

    @Test
    @DisplayName("Sin paciente o sin número no se llama a ms-patients")
    void skipsWhenArgumentsMissing() {
        PatientClientAdapter adapter = adapter("{}", HttpStatus.OK);

        StepVerifier.create(adapter.assignMedicalRecordNumber(null, "HC-00000001")).verifyComplete();
        StepVerifier.create(adapter.assignMedicalRecordNumber("p-1", " ")).verifyComplete();

        assertThat(requests).isEmpty();
    }
}

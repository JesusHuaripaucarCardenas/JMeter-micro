package pe.edu.vallegrande.sigrc.medicalhistory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class MedicalhistoryApplication {

	/**
	 * Zona horaria de la clínica. El contenedor corre en UTC, así que sin esto
	 * LocalDateTime.now() (fecha/hora de consulta, created_at, edad) salía 5 h
	 * adelantada respecto a la hora de Lima.
	 */
	private static final String ZONE_ID = "America/Lima";

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(ZONE_ID));
		SpringApplication.run(MedicalhistoryApplication.class, args);
	}

}

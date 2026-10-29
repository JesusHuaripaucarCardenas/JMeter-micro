package pe.edu.vallegrande.sigrc.medicalhistory.domain.exception;

public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}

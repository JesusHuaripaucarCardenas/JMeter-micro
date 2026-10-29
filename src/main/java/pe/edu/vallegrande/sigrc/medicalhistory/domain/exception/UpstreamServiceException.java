package pe.edu.vallegrande.sigrc.medicalhistory.domain.exception;

/**
 * Señala que una llamada a un microservicio externo (ms-patients, ms-doctors,
 * etc.) falló por una razón que NO es "el recurso no existe" — por ejemplo un
 * 401/403 (token no propagado o inválido), un 5xx del servicio externo, un
 * timeout o un problema de red/DNS.
 *
 * Se diferencia deliberadamente de {@link NotFoundException}: mezclar ambos
 * casos bajo el mismo resultado (como ocurría antes, donde cualquier error
 * de la llamada HTTP se traducía silenciosamente en "no encontrado") hace
 * que fallas de infraestructura se disfracen de "el paciente no existe" de
 * cara al usuario, lo cual es engañoso y dificulta el diagnóstico.
 */
public class UpstreamServiceException extends DomainException {
    public UpstreamServiceException(String message, Throwable cause) {
        super(message);
        if (cause != null) {
            initCause(cause);
        }
    }
}

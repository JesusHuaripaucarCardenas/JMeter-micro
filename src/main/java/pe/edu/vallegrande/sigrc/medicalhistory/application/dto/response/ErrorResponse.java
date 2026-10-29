package pe.edu.vallegrande.sigrc.medicalhistory.application.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ErrorResponse {
    private String        error;
    private String        message;
    private int           status;
    private LocalDateTime timestamp;
}

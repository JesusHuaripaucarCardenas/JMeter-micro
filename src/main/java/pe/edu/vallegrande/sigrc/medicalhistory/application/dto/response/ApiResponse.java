package pe.edu.vallegrande.sigrc.medicalhistory.application.dto.response;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String  message;
    private T       data;
}

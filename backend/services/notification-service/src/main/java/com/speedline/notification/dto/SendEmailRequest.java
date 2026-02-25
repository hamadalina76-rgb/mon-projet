package com.speedline.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for admin sending email to a user (e.g. client).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailRequest {

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Subject is required")
    private String subject;

    /**
     * Plain text or HTML body. Used when templateName is null.
     */
    private String body;

    /**
     * Optional template name (e.g. "generic-message"). If set, variables are used.
     */
    private String templateName;

    /**
     * Variables for the template (e.g. {"body": "Hello..."}).
     */
    private Map<String, Object> variables;
}

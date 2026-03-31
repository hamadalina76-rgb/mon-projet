package com.speedline.partner.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteCreateRequest {

    @NotBlank(message = "partnerId is required")
    private String partnerId;
}

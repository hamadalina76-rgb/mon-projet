package com.speedline.partner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteResponse {

    private Long id;
    private Long customerId;
    private Long partnerId;
    private LocalDateTime createdAt;
}

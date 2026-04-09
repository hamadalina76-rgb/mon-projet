package com.speedline.order.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSnapshot {
    private Long id;
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
}

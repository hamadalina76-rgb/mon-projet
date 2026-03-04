package com.speedline.partner.dto;

import com.speedline.partner.domain.StaffRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffMemberDTO {
    private Long id;
    private Long partnerId;
    private Long userId;
    private StaffRole role;
}

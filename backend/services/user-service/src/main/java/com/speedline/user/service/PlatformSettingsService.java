package com.speedline.user.service;

import com.speedline.user.dto.PlatformSettingsDTO;

public interface PlatformSettingsService {

    PlatformSettingsDTO.GeneralSettingsResponse getGeneralSettings();

    PlatformSettingsDTO.GeneralSettingsResponse updateGeneralSettings(
            PlatformSettingsDTO.GeneralSettingsRequest request, Long adminId);

    /** Public endpoint — no auth required (used by customer_app / courier_app). */
    PlatformSettingsDTO.AppStatusResponse getAppStatus();
}

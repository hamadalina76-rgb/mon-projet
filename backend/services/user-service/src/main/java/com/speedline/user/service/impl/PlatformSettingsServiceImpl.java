package com.speedline.user.service.impl;

import com.speedline.user.domain.PlatformSetting;
import com.speedline.user.dto.AppWorkingHoursDTO;
import com.speedline.user.dto.PlatformSettingsDTO;
import com.speedline.user.repository.PlatformSettingRepository;
import com.speedline.user.service.AppWorkingHoursService;
import com.speedline.user.service.PlatformSettingsService;
import com.speedline.user.service.SettingsAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformSettingsServiceImpl implements PlatformSettingsService {

    private final PlatformSettingRepository repository;
    private final SettingsAuditLogService   auditLogService;
    private final AppWorkingHoursService    workingHoursService;

    // ── Helpers ───────────────────────────────────────────────────────────

    private String getValue(String key, String defaultValue) {
        return repository.findBySettingKey(key)
                .map(PlatformSetting::getSettingValue)
                .orElse(defaultValue);
    }

    private void setValue(String key, String value, String description) {
        PlatformSetting setting = repository.findBySettingKey(key)
                .orElseGet(() -> PlatformSetting.builder()
                        .settingKey(key)
                        .description(description)
                        .build());
        setting.setSettingValue(value);
        repository.save(setting);
    }

    // ── Read ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PlatformSettingsDTO.GeneralSettingsResponse getGeneralSettings() {
        return PlatformSettingsDTO.GeneralSettingsResponse.builder()
                .platformName(getValue("PLATFORM_NAME", "SpeedLine"))
                .contactEmail(getValue("CONTACT_EMAIL", ""))
                .contactPhone(getValue("CONTACT_PHONE", ""))
                .defaultLanguage(getValue("DEFAULT_LANGUAGE", "fr"))
                .defaultCurrency(getValue("DEFAULT_CURRENCY", "MAD"))
                .maintenanceMode("true".equalsIgnoreCase(getValue("MAINTENANCE_MODE", "false")))
                .appEnabled("true".equalsIgnoreCase(getValue("APP_ENABLED", "true")))
                .build();
    }

    // ── Update ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PlatformSettingsDTO.GeneralSettingsResponse updateGeneralSettings(
            PlatformSettingsDTO.GeneralSettingsRequest req, Long adminId) {

        if (req.getPlatformName() != null)
            setValue("PLATFORM_NAME", req.getPlatformName(), "Nom de la plateforme");
        if (req.getContactEmail() != null)
            setValue("CONTACT_EMAIL", req.getContactEmail(), "E-mail de contact");
        if (req.getContactPhone() != null)
            setValue("CONTACT_PHONE", req.getContactPhone(), "Téléphone de contact");
        if (req.getDefaultLanguage() != null)
            setValue("DEFAULT_LANGUAGE", req.getDefaultLanguage(), "Langue par défaut");
        if (req.getDefaultCurrency() != null)
            setValue("DEFAULT_CURRENCY", req.getDefaultCurrency(), "Devise par défaut");
        if (req.getMaintenanceMode() != null)
            setValue("MAINTENANCE_MODE", req.getMaintenanceMode().toString(), "Mode maintenance");
        if (req.getAppEnabled() != null) {
            setValue("APP_ENABLED", req.getAppEnabled().toString(), "Application activée/désactivée");
            String action = Boolean.TRUE.equals(req.getAppEnabled()) ? "APP_ENABLED" : "APP_DISABLED";
            auditLogService.log(action, adminId,
                    Boolean.TRUE.equals(req.getAppEnabled()) ? "Application réactivée" : "Application désactivée");
        } else {
            auditLogService.log("GENERAL_SETTINGS_UPDATED", adminId, "Paramètres généraux modifiés");
        }

        log.info("Platform settings updated");
        return getGeneralSettings();
    }

    // ── Public status endpoint ────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PlatformSettingsDTO.AppStatusResponse getAppStatus() {
        AppWorkingHoursDTO.TodayStatusResponse today = workingHoursService.getTodayStatus();
        return PlatformSettingsDTO.AppStatusResponse.builder()
                .appEnabled("true".equalsIgnoreCase(getValue("APP_ENABLED", "true")))
                .maintenanceMode("true".equalsIgnoreCase(getValue("MAINTENANCE_MODE", "false")))
                .currentlyOpen(today.isCurrentlyOpen())
                .currentDay(today.getCurrentDay())
                .todayOpenTime(today.getOpenTime())
                .todayCloseTime(today.getCloseTime())
                .todayIsOpen(today.isTodayIsOpen())
                .build();
    }
}

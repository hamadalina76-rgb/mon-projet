package com.speedline.user.repository;

import com.speedline.user.domain.PlatformSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, Long> {

    Optional<PlatformSetting> findBySettingKey(String settingKey);

    List<PlatformSetting> findAllByOrderBySettingKeyAsc();
}

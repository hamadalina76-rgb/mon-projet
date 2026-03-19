package com.speedline.partner.repository;

import com.speedline.partner.domain.PartnerZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerZoneRepository extends JpaRepository<PartnerZone, Long> {

    List<PartnerZone> findByPartnerId(Long partnerId);

    boolean existsByPartnerIdAndZoneId(Long partnerId, Long zoneId);

    @Modifying
    @Query("DELETE FROM PartnerZone pz WHERE pz.partnerId = :partnerId AND pz.zoneId = :zoneId")
    void deleteByPartnerIdAndZoneId(@Param("partnerId") Long partnerId, @Param("zoneId") Long zoneId);

    @Modifying
    @Query("DELETE FROM PartnerZone pz WHERE pz.partnerId = :partnerId")
    void deleteAllByPartnerId(@Param("partnerId") Long partnerId);
}

package com.speedline.partner.repository;

import com.speedline.partner.domain.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Optional<Favorite> findByCustomerIdAndPartnerId(Long customerId, Long partnerId);

    boolean existsByCustomerIdAndPartnerId(Long customerId, Long partnerId);
}

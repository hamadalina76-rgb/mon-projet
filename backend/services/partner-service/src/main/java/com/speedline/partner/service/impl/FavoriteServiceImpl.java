package com.speedline.partner.service.impl;

import com.speedline.partner.domain.Favorite;
import com.speedline.partner.domain.Partner;
import com.speedline.partner.dto.response.FavoriteResponse;
import com.speedline.partner.dto.response.FavoriteUpsertResult;
import com.speedline.partner.exception.PartnerNotFoundException;
import com.speedline.partner.exception.ResourceNotFoundException;
import com.speedline.partner.repository.FavoriteRepository;
import com.speedline.partner.repository.PartnerRepository;
import com.speedline.partner.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final PartnerRepository partnerRepository;

    @Override
    public List<FavoriteResponse> getFavorites(Long customerId) {
        final List<Favorite> favorites = favoriteRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        final List<FavoriteResponse> responses = new ArrayList<>();

        for (Favorite favorite : favorites) {
            final Optional<Partner> partner = partnerRepository.findById(favorite.getPartnerId());
            if (partner.isEmpty() || !Boolean.TRUE.equals(partner.get().getIsActive())) {
                // Partner was removed/disabled: silently remove stale favorite.
                favoriteRepository.delete(favorite);
                continue;
            }
            responses.add(toResponse(favorite));
        }

        return responses;
    }

    @Override
    public FavoriteUpsertResult addFavorite(Long customerId, Long partnerId) {
        ensurePartnerExistsAndActive(partnerId);

        final Optional<Favorite> existing = favoriteRepository.findByCustomerIdAndPartnerId(customerId, partnerId);
        if (existing.isPresent()) {
            return new FavoriteUpsertResult(toResponse(existing.get()), false);
        }

        try {
            final Favorite created = favoriteRepository.save(
                Favorite.builder()
                    .customerId(customerId)
                    .partnerId(partnerId)
                    .build()
            );
            return new FavoriteUpsertResult(toResponse(created), true);
        } catch (DataIntegrityViolationException ex) {
            // Race condition safety for idempotency.
            final Favorite fallback = favoriteRepository
                .findByCustomerIdAndPartnerId(customerId, partnerId)
                .orElseThrow(() -> ex);
            return new FavoriteUpsertResult(toResponse(fallback), false);
        }
    }

    @Override
    public void removeFavorite(Long customerId, Long partnerId) {
        final Favorite favorite = favoriteRepository
            .findByCustomerIdAndPartnerId(customerId, partnerId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Favorite not found for partnerId: " + partnerId
            ));

        favoriteRepository.delete(favorite);
    }

    private void ensurePartnerExistsAndActive(Long partnerId) {
        final Partner partner = partnerRepository.findById(partnerId)
            .orElseThrow(() -> new PartnerNotFoundException("Partner not found with id: " + partnerId));

        if (!Boolean.TRUE.equals(partner.getIsActive())) {
            throw new PartnerNotFoundException("Partner not found with id: " + partnerId);
        }
    }

    private FavoriteResponse toResponse(Favorite favorite) {
        return FavoriteResponse.builder()
            .id(favorite.getId())
            .customerId(favorite.getCustomerId())
            .partnerId(favorite.getPartnerId())
            .createdAt(favorite.getCreatedAt())
            .build();
    }
}

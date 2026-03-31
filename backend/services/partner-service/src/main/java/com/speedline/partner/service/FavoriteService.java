package com.speedline.partner.service;

import com.speedline.partner.dto.response.FavoriteResponse;
import com.speedline.partner.dto.response.FavoriteUpsertResult;

import java.util.List;

public interface FavoriteService {

    List<FavoriteResponse> getFavorites(Long customerId);

    FavoriteUpsertResult addFavorite(Long customerId, Long partnerId);

    void removeFavorite(Long customerId, Long partnerId);
}

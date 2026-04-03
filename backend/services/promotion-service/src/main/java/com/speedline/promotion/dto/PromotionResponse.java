package com.speedline.promotion.dto;

/** Generic API wrapper returned by the controller. */
public record PromotionResponse<T>(boolean success, String message, T data) {

    public static <T> PromotionResponse<T> ok(T data) {
        return new PromotionResponse<>(true, null, data);
    }

    public static <T> PromotionResponse<T> ok(String message, T data) {
        return new PromotionResponse<>(true, message, data);
    }

    public static <T> PromotionResponse<T> error(String message) {
        return new PromotionResponse<>(false, message, null);
    }
}

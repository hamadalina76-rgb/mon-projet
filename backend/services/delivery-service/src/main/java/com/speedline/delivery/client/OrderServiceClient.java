package com.speedline.delivery.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @GetMapping("/orders/{id}")
    Map<String, Object> getOrderById(@PathVariable("id") Long orderId);

    @GetMapping("/orders/awaiting-courier")
    List<Map<String, Object>> getOrdersAwaitingCourier();

    @DeleteMapping("/orders/{id}")
    void cancelOrder(@PathVariable("id") Long orderId, @RequestBody CancelOrderRequest request);

    @PostMapping("/orders/{id}/refund")
    void refundOrder(@PathVariable("id") Long orderId, @RequestBody RefundOrderRequest request);

    record CancelOrderRequest(String cancelledBy, Long actorId, String reason) {}

    record RefundOrderRequest(BigDecimal amount) {}
}

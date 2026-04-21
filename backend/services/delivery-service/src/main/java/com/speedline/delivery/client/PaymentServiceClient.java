package com.speedline.delivery.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "payment-service")
public interface PaymentServiceClient {

    @PostMapping("/wallets/{userId}/credit")
    void creditCustomer(@PathVariable("userId") Long userId, @RequestBody CreditRequest req);

    @PostMapping("/payments/wallet/credits")
    void creditWallet(@RequestBody WalletCreditRequest request);

    record CreditRequest(BigDecimal amount, String currency, String referenceId, String reason) {
    }

    record WalletCreditRequest(Long customerId,
                               Long orderId,
                               Long bundleId,
                               Double amount,
                               String currency,
                               String reason) {
    }
}

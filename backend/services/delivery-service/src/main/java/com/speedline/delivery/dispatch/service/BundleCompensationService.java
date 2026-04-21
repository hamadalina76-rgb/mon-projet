package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.client.PaymentServiceClient;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BundleCompensationService {

    private final PaymentServiceClient paymentServiceClient;
    private final DispatchProperties dispatchProperties;

    public void compensateIfBundleLate(PendingOrder order, Long bundleId, Integer etaDeliveryMinutes) {
        if (order == null || bundleId == null || etaDeliveryMinutes == null) {
            return;
        }
        if (!dispatchProperties.getCompensation().isEnabled()) {
            return;
        }
        Integer guaranteed = order.getGuaranteedDeliveryMinutes();
        if (guaranteed == null) {
            return;
        }
        int allowed = guaranteed + dispatchProperties.getCompensation().getGraceDelayMinutes();
        if (etaDeliveryMinutes <= allowed) {
            return;
        }

        try {
            paymentServiceClient.creditWallet(new PaymentServiceClient.WalletCreditRequest(
                    order.getCustomerId(),
                    order.getId(),
                    bundleId,
                    dispatchProperties.getCompensation().getWalletCreditAmount(),
                    dispatchProperties.getCompensation().getWalletCreditCurrency(),
                    "BUNDLE_DELAY_COMPENSATION"
            ));
        } catch (Exception ex) {
            log.warn("Bundle compensation failed orderId={} bundleId={}: {}",
                    order.getId(), bundleId, ex.getMessage());
        }
    }
}

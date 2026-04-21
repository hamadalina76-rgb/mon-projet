package com.speedline.delivery.matching.cost.component;

import com.speedline.delivery.matching.cost.model.CostComponentKey;
import com.speedline.delivery.matching.cost.model.CostContribution;
import com.speedline.delivery.matching.cost.model.DispatchComponentConfig;
import com.speedline.delivery.matching.cost.model.ETAEstimation;
import com.speedline.delivery.matching.cost.model.ScoringContext;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class MerchantKnowledgeCostComponent extends BaseCostComponent {

    @Override
    public CostComponentKey key() {
        return CostComponentKey.MERCHANT_KNOWLEDGE;
    }

    @Override
    public CostContribution evaluate(ScoringContext context, DispatchComponentConfig config, ETAEstimation eta) {
        LocalDateTime lastMerchantDelivery = context.getLastMerchantDeliveryAt();
        if (lastMerchantDelivery == null) {
            return score(config, 0.0, "Pas d'historique commerce");
        }

        long hours = Duration.between(lastMerchantDelivery, LocalDateTime.now()).toHours();
        if (hours >= 0 && hours < 24 * 7) {
            return score(config, -10.0, "Connaissance récente du commerce");
        }
        return score(config, 0.0, "Connaissance commerce ancienne");
    }
}

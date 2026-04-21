package com.speedline.delivery.compensation;

import com.speedline.delivery.client.OrderServiceClient;
import com.speedline.delivery.client.PaymentServiceClient;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.metrics.DispatchMetrics;
import com.speedline.delivery.domain.Delivery;
import com.speedline.delivery.repository.DeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LateDeliveryCompensationServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;
    @Mock
    private PaymentServiceClient paymentServiceClient;
    @Mock
    private OrderServiceClient orderServiceClient;
    @Mock
    private DispatchMetrics dispatchMetrics;

    private LateDeliveryCompensationService service;

    @BeforeEach
    void setUp() {
        DispatchProperties props = new DispatchProperties();
        props.getBundling().getCompensation().setEnabled(true);
        props.getBundling().getCompensation().setThresholdMinutes(10);
        props.getBundling().getCompensation().setPercentOfTotal(0.10);
        props.getBundling().getCompensation().setCurrency("TND");
        service = new LateDeliveryCompensationService(props, deliveryRepository, paymentServiceClient, orderServiceClient, dispatchMetrics);
    }

    @Test
    void belowThreshold_noOp() {
        Delivery delivery = bundledDelivery();
        delivery.setDeliveredAt(delivery.getPromisedDeliveryAt().plusMinutes(9));

        service.evaluateAndCompensate(delivery);

        verify(paymentServiceClient, never()).creditCustomer(any(), any());
    }

    @Test
    void nonBundled_noOp() {
        Delivery delivery = bundledDelivery();
        delivery.setBundleId(null);

        service.evaluateAndCompensate(delivery);

        verify(paymentServiceClient, never()).creditCustomer(any(), any());
    }

    @Test
    void idempotent_secondCallNoOp() {
        Delivery delivery = bundledDelivery();
        delivery.setCompensationIssuedAt(LocalDateTime.now());

        service.evaluateAndCompensate(delivery);

        verify(paymentServiceClient, never()).creditCustomer(any(), any());
    }

    @Test
    void percentageCalculation_callsPayment() {
        Delivery delivery = bundledDelivery();
        delivery.setDeliveredAt(delivery.getPromisedDeliveryAt().plusMinutes(12));
        when(orderServiceClient.getOrderById(99L)).thenReturn(Map.of("total", 100.0, "customerId", 88L));

        service.evaluateAndCompensate(delivery);

        verify(paymentServiceClient).creditCustomer(any(), any());
        verify(deliveryRepository).save(delivery);
    }

    @Test
    void feignException_noCompensationIssuedAt() {
        Delivery delivery = bundledDelivery();
        delivery.setDeliveredAt(delivery.getPromisedDeliveryAt().plusMinutes(12));
        when(orderServiceClient.getOrderById(99L)).thenReturn(Map.of("total", 100.0, "customerId", 88L));
        org.mockito.Mockito.doThrow(new RuntimeException("down"))
                .when(paymentServiceClient).creditCustomer(any(), any());

        service.evaluateAndCompensate(delivery);

        verify(deliveryRepository, never()).save(delivery);
        verify(dispatchMetrics).recordBundleCompensationFailed();
    }

    private static Delivery bundledDelivery() {
        Delivery delivery = new Delivery();
        delivery.setId(42L);
        delivery.setOrderId(99L);
        delivery.setBundleId(7L);
        delivery.setPromisedDeliveryAt(LocalDateTime.now().minusMinutes(20));
        delivery.setDeliveredAt(LocalDateTime.now());
        return delivery;
    }
}

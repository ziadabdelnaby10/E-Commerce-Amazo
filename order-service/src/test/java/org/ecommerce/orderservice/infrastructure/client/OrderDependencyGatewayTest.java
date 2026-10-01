package org.ecommerce.orderservice.infrastructure.client;

import org.ecommerce.orderservice.domain.dto.response.GeneralResponse;
import org.ecommerce.orderservice.domain.model.Order;
import org.ecommerce.orderservice.domain.model.OrderItem;
import org.ecommerce.orderservice.infrastructure.client.dto.InitiatePaymentResponse;
import org.ecommerce.orderservice.infrastructure.client.dto.ReserveInventoryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDependencyGatewayTest {

    @Mock
    private InventoryClient inventoryClient;
    @Mock
    private PaymentClient paymentClient;
    @Mock
    private CustomerClient customerClient;

    private OrderDependencyGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new OrderDependencyGateway(inventoryClient, paymentClient, customerClient);
    }

    @Test
    void initiatePaymentAcceptsCreatedResponses() {
        when(paymentClient.initiatePayment(any()))
                .thenReturn(GeneralResponse.of(201, new InitiatePaymentResponse("PAY-1", "AUTHORIZED", null)));

        InitiatePaymentResponse response = gateway.initiatePayment(sampleOrder());

        assertThat(response.paymentId()).isEqualTo("PAY-1");
        assertThat(response.accepted()).isTrue();
    }

    @Test
    void initiatePaymentReturnsFailureWhenDownstreamPayloadIsMissing() {
        when(paymentClient.initiatePayment(any()))
                .thenReturn(GeneralResponse.of(201, null));

        InitiatePaymentResponse response = gateway.initiatePayment(sampleOrder());

        assertThat(response.accepted()).isFalse();
        assertThat(response.reason()).isEqualTo("Payment initiation failed");
    }

    @Test
    void reserveInventoryReturnsFailureInsteadOfNullWhenDownstreamRejectsRequest() {
        when(inventoryClient.reserveInventory(any()))
                .thenReturn(GeneralResponse.of(409, ReserveInventoryResponse.failed("Lock contended")));

        ReserveInventoryResponse response = gateway.reserveInventory(sampleOrder());

        assertThat(response).isNotNull();
        assertThat(response.reserved()).isFalse();
        assertThat(response.reason()).isEqualTo("Inventory reservation failed");
    }

    @Test
    void findCustomerReturnsEmptyWhenLookupFails() {
        when(customerClient.findById("user-1")).thenThrow(new RuntimeException("boom"));

        Optional<?> response = gateway.findCustomer("user-1");

        assertThat(response).isEmpty();
    }

    private Order sampleOrder() {
        OrderItem item = new OrderItem();
        item.setProductId(5L);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("10.00"));

        Order order = new Order();
        order.setId(11L);
        order.setUserId("user-1");
        order.setCurrency("USD");
        order.setTotalAmount(new BigDecimal("20.00"));
        order.setCustomerEmail("user1@example.com");
        order.setItems(Set.of(item));
        return order;
    }
}


package org.ecommerce.orderservice.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.orderservice.domain.model.Order;
import org.ecommerce.orderservice.infrastructure.client.dto.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDependencyGateway {

    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final CustomerClient customerClient;

    public Boolean checkCustomerExist(String customerId) {
        try {
            return customerClient.existsById(customerId);
        } catch (Exception ex) {
            log.warn("Checking Customer Failed for Customer Id {}", customerId, ex);
        }
        return false;
    }

    public ReserveInventoryResponse reserveInventory(Order order) {
        try {
            return inventoryClient.reserveInventory(new ReserveInventoryRequest(order.getId(), toInventoryItems(order)));
        } catch (Exception ex) {
            return reserveInventoryFallback(order, ex);
        }
    }

    public void releaseInventory(Order order) {
        try {
            inventoryClient.releaseInventory(new ReleaseInventoryRequest(order.getId(), toInventoryItems(order)));
        } catch (Exception ex) {
            releaseInventoryFallback(order, ex);
        }
    }

    public InitiatePaymentResponse initiatePayment(Order order) {
        try {
            return paymentClient.initiatePayment(new InitiatePaymentRequest(
                    order.getId(),
                    order.getUserId(),
                    order.getTotalAmount(),
                    order.getCurrency()
            ));
        } catch (Exception ex) {
            return initiatePaymentFallback(order, ex);
        }
    }

    private void reserveCustomerFallback(String customerId, Exception ex) {
        log.warn("Customer Information fallback for Customer Id {}", customerId, ex);
    }

    private ReserveInventoryResponse reserveInventoryFallback(Order order, Throwable throwable) {
        log.warn("Inventory reservation fallback for order {}", order.getId(), throwable);
        return ReserveInventoryResponse.failed("Inventory service unavailable");
    }

    private void releaseInventoryFallback(Order order, Throwable throwable) {
        log.warn("Inventory release fallback for order {}", order.getId(), throwable);
    }

    private InitiatePaymentResponse initiatePaymentFallback(Order order, Throwable throwable) {
        log.warn("Payment initiation fallback for order {}", order.getId(), throwable);
        return InitiatePaymentResponse.failed("Payment service unavailable");
    }

    private List<ReserveInventoryItemRequest> toInventoryItems(Order order) {
        return order.getItems().stream()
                .map(item -> new ReserveInventoryItemRequest(item.getProductId(), item.getQuantity()))
                .toList();
    }
}



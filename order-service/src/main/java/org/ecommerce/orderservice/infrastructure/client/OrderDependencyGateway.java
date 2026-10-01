package org.ecommerce.orderservice.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.orderservice.domain.dto.response.GeneralResponse;
import org.ecommerce.orderservice.domain.model.Order;
import org.ecommerce.orderservice.infrastructure.client.dto.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Facade over downstream customer, inventory, and payment clients.
 *
 * <p>The order application service depends on this gateway rather than individual Feign clients
 * so downstream fallback behavior, logging, and payload translation remain centralized.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDependencyGateway {

    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final CustomerClient customerClient;

    /**
     * Checks whether the referenced customer exists.
     *
     * <p>This is the strict validation step for order creation. Failures are treated as a
     * negative result because a new order cannot be created without a valid customer.</p>
     */
    public Boolean checkCustomerExist(String customerId) {
        try {
            log.info("Checking Customer Exist for Customer Id {}", customerId);
            log.info("Customer Client: {}", customerClient.existsById(customerId));
            return customerClient.existsById(customerId).data();
        } catch (Exception ex) {
            log.warn("Checking Customer Failed for Customer Id {}", customerId, ex);
        }
        return false;
    }

    /**
     * Fetches the customer so the order can snapshot their email address.
     *
     * <p>The email is embedded in every emitted order event, which lets notification-service work
     * purely from the event payload instead of calling customer-service from a Kafka consumer
     * thread where no caller token is available to relay.</p>
     *
     * @return the customer, or empty when the lookup fails; order creation must not depend on it
     */
    public Optional<CustomerResponse> findCustomer(String customerId) {
        try {
            return Optional.ofNullable(customerClient.findById(customerId).data());
        } catch (Exception ex) {
            reserveCustomerFallback(customerId, ex);
            return Optional.empty();
        }
    }

    /**
     * Calls inventory-service to reserve stock for the order items.
     */
    public ReserveInventoryResponse reserveInventory(Order order) {
        try {
            GeneralResponse<ReserveInventoryResponse> response = inventoryClient.reserveInventory(new ReserveInventoryRequest(order.getId(), toInventoryItems(order)));
            log.info("Inventory reservation response for order {}: {}", order.getId(), response);
            if (hasFailedStatus(response) || response.data() == null) {
                log.warn("Inventory reservation failed for order {}: statusCode={}, payload={}",
                        order.getId(), response == null ? null : response.statusCode(), response == null ? null : response.data());
                return ReserveInventoryResponse.failed("Inventory reservation failed");
            }
            return response.data();
        } catch (Exception ex) {
            return reserveInventoryFallback(order, ex);
        }
    }

    /**
     * Calls inventory-service to release previously reserved stock.
     */
    public void releaseInventory(Order order) {
        try {
            inventoryClient.releaseInventory(new ReleaseInventoryRequest(order.getId(), toInventoryItems(order)));
        } catch (Exception ex) {
            releaseInventoryFallback(order, ex);
        }
    }

    /**
     * Calls payment-service to start payment processing for the order.
     */
    public InitiatePaymentResponse initiatePayment(Order order) {
        try {
            GeneralResponse<InitiatePaymentResponse> response = paymentClient.initiatePayment(new InitiatePaymentRequest(
                    order.getId(),
                    order.getUserId(),
                    order.getTotalAmount(),
                    order.getCurrency(),
                    order.getCustomerEmail()
            ));
            log.info("Payment initiation response for order {}: {}", order.getId(), response);
            if (hasFailedStatus(response) || response.data() == null) {
                log.warn("Payment initiation failed for order {}: statusCode={}, payload={}",
                        order.getId(), response == null ? null : response.statusCode(), response == null ? null : response.data());
                return InitiatePaymentResponse.failed("Payment initiation failed");
            }
            return response.data();
        } catch (Exception ex) {
            return initiatePaymentFallback(order, ex);
        }
    }

    /**
     * Logs a degraded-mode customer lookup failure.
     */
    private void reserveCustomerFallback(String customerId, Exception ex) {
        log.warn("Customer Information fallback for Customer Id {}", customerId, ex);
    }

    /**
     * Converts inventory failures into a uniform negative reservation response.
     */
    private ReserveInventoryResponse reserveInventoryFallback(Order order, Throwable throwable) {
        log.warn("Inventory reservation fallback for order {}", order.getId(), throwable);
        return ReserveInventoryResponse.failed("Inventory service unavailable");
    }

    /**
     * Logs inventory release failures so they can be retried or repaired operationally.
     */
    private void releaseInventoryFallback(Order order, Throwable throwable) {
        log.warn("Inventory release fallback for order {}", order.getId(), throwable);
    }

    /**
     * Converts payment initiation failures into a uniform negative response.
     */
    private InitiatePaymentResponse initiatePaymentFallback(Order order, Throwable throwable) {
        log.warn("Payment initiation fallback for order {}", order.getId(), throwable);
        return InitiatePaymentResponse.failed("Payment service unavailable");
    }

    /**
     * Maps order items into the inventory-service reservation DTO shape.
     */
    private List<ReserveInventoryItemRequest> toInventoryItems(Order order) {
        return order.getItems().stream()
                .map(item -> new ReserveInventoryItemRequest(item.getProductId(), item.getQuantity()))
                .toList();
    }

    private boolean hasFailedStatus(GeneralResponse<?> response) {
        return response == null || response.statusCode() < 200 || response.statusCode() >= 300;
    }
}



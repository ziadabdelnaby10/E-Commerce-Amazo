package org.ecommerce.orderservice.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ecommerce.orderservice.domain.dto.AddressDto;
import org.ecommerce.orderservice.domain.dto.request.CreateOrderItemRequest;
import org.ecommerce.orderservice.domain.dto.request.CreateOrderRequest;
import org.ecommerce.orderservice.domain.dto.response.OrderResponse;
import org.ecommerce.orderservice.service.implementation.OrderServiceImpl;
import org.ecommerce.orderservice.infrastructure.client.OrderDependencyGateway;
import org.ecommerce.orderservice.infrastructure.client.dto.InitiatePaymentResponse;
import org.ecommerce.orderservice.infrastructure.client.dto.ReserveInventoryResponse;
import org.ecommerce.orderservice.domain.model.IdempotencyKey;
import org.ecommerce.orderservice.domain.model.Order;
import org.ecommerce.orderservice.domain.model.OrderItem;
import org.ecommerce.orderservice.domain.model.OrderStatus;
import org.ecommerce.orderservice.domain.model.PaymentStatus;
import org.ecommerce.orderservice.infrastructure.mapping.OrderMapper;
import org.ecommerce.orderservice.infrastructure.persistence.repository.IdempotencyKeyJpaRepository;
import org.ecommerce.orderservice.infrastructure.persistence.repository.OrderEventJpaRepository;
import org.ecommerce.orderservice.infrastructure.persistence.repository.OrderJpaRepository;
import org.ecommerce.orderservice.infrastructure.persistence.repository.OrderStatusHistoryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderJpaRepository orderRepository;
    @Mock
    private OrderEventJpaRepository orderEventRepository;
    @Mock
    private OrderStatusHistoryJpaRepository statusHistoryRepository;
    @Mock
    private IdempotencyKeyJpaRepository idempotencyRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderDependencyGateway dependencyGateway;

    private OrderServiceImpl service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new OrderServiceImpl(
                orderRepository,
                orderEventRepository,
                statusHistoryRepository,
                idempotencyRepository,
                orderMapper,
                dependencyGateway,
                objectMapper
        );
    }

    @Test
    void createOrderReturnsCachedResponseWhenIdempotencyKeyAlreadyProcessed() {
        OrderResponse cached = new OrderResponse(
                7L,
                "ORD-ABCD1234",
                "6a5e87573c810cff28852bfc",
                OrderStatus.PENDING,
                PaymentStatus.PENDING,
                BigDecimal.TEN,
                "USD",
                new AddressDto("s", "c", "st", "z", "co"),
                null,
                null,
                Instant.now(),
                Instant.now(),
                List.of()
        );

        IdempotencyKey existing = new IdempotencyKey();
        existing.setIdempotencyKey("k1");
        existing.setResponseBody(objectMapper.valueToTree(cached));

        when(idempotencyRepository.findByIdempotencyKey("k1")).thenReturn(Optional.of(existing));

        OrderResponse result = service.createOrder("6a5e87573c810cff28852bfc", "k1", sampleRequest());

        assertThat(result.orderNumber()).isEqualTo("ORD-ABCD1234");
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrderPersistsOrderStatusHistoryAndOutboxEvent() {
        when(idempotencyRepository.findByIdempotencyKey("k2")).thenReturn(Optional.empty());
        when(idempotencyRepository.save(any(IdempotencyKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order mappedOrder = new Order();
        OrderItem item = new OrderItem();
        item.setProductId(99L);
        item.setProductName("Keyboard");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("25.00"));
        mappedOrder.setItems(Set.of(item));

        when(orderMapper.toOrder(any(CreateOrderRequest.class))).thenReturn(mappedOrder);
        when(dependencyGateway.reserveInventory(any(Order.class))).thenReturn(ReserveInventoryResponse.success());
        when(dependencyGateway.initiatePayment(any(Order.class))).thenReturn(new InitiatePaymentResponse("p-1", "PENDING", null));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(orderMapper.toResponse(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            return new OrderResponse(
                    saved.getId(),
                    saved.getOrderNumber(),
                    saved.getUserId(),
                    saved.getStatus(),
                    saved.getPaymentStatus(),
                    saved.getTotalAmount(),
                    saved.getCurrency(),
                    null,
                    null,
                    saved.getNotes(),
                    saved.getCreatedAt(),
                    saved.getUpdatedAt(),
                    List.of()
            );
        });

        OrderResponse result = service.createOrder("6a5e87573c810cff28852bfc", "k2", sampleRequest());

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.totalAmount()).isEqualByComparingTo("50.00");
        verify(statusHistoryRepository, times(1)).save(any());
        verify(orderEventRepository, times(1)).save(any());
    }

    private CreateOrderRequest sampleRequest() {
        return new CreateOrderRequest(
                new AddressDto("Main", "Cairo", "Cairo", "12345", "EG"),
                null,
                "test",
                List.of(new CreateOrderItemRequest(99L, "Keyboard", 2, new BigDecimal("25.00")))
        );
    }
}


package org.ecommerce.orderservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ecommerce.orderservice.domain.dto.request.CreateOrderRequest;
import org.ecommerce.orderservice.domain.dto.response.GeneralResponse;
import org.ecommerce.orderservice.domain.dto.response.OrderResponse;
import org.ecommerce.orderservice.domain.dto.response.OrderSummaryResponse;
import org.ecommerce.orderservice.service.OrderService;
import org.ecommerce.orderservice.domain.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/orders")
public class OrderController {

    private final OrderService orderService;

    /**
     * The caller identity is taken from the validated JWT, never from a client supplied header:
     * an {@code X-User-Id} header can be forged by anyone able to reach the service directly.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_ORDER')")
    public ResponseEntity<GeneralResponse<OrderResponse>> createOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        OrderResponse response = orderService.createOrder(currentUserId(jwt), idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GeneralResponse.of(HttpStatus.CREATED.value(), response));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAuthority('VIEW_ORDERS')")
    public ResponseEntity<GeneralResponse<OrderResponse>> getById(@PathVariable Long orderId) {
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), orderService.getById(orderId)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_ORDERS') and (hasAuthority('VIEW_USERS') "
            + "or (authentication.principal instanceof T(org.springframework.security.oauth2.jwt.Jwt) "
            + "and authentication.principal.claims['userId'] == #userId.toString()))")
    public ResponseEntity<GeneralResponse<Page<OrderSummaryResponse>>> listByUser(
            @RequestParam Long userId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<OrderSummaryResponse> response = orderService.listByUser(userId, status, PageRequest.of(page, size));
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), response));
    }

    private static String currentUserId(Jwt jwt) {
        String userId = jwt == null ? null : jwt.getClaimAsString("userId");
        if (userId == null || userId.isBlank()) {
            throw new AccessDeniedException("Access token does not contain a userId claim");
        }
        return userId;
    }
}

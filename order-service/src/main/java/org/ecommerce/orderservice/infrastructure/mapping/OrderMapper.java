package org.ecommerce.orderservice.infrastructure.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ecommerce.orderservice.domain.dto.AddressDto;
import org.ecommerce.orderservice.domain.dto.request.CreateOrderItemRequest;
import org.ecommerce.orderservice.domain.dto.request.CreateOrderRequest;
import org.ecommerce.orderservice.domain.dto.response.OrderResponse;
import org.ecommerce.orderservice.domain.dto.response.OrderSummaryResponse;
import org.ecommerce.orderservice.domain.model.Order;
import org.ecommerce.orderservice.domain.model.OrderItem;
import org.ecommerce.orderservice.infrastructure.persistence.projection.OrderSummaryProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class OrderMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderNumber", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "paymentStatus", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "items", source = "items")
    public abstract Order toOrder(CreateOrderRequest request);

    @Mapping(target = "id" , source = "id")
    @Mapping(target = "orderNumber" , source = "orderNumber")
    @Mapping(target = "status" , source = "status")
    @Mapping(target = "paymentStatus" , source = "paymentStatus")
    @Mapping(target = "totalAmount" , source = "totalAmount")
    @Mapping(target = "currency" , source = "currency")
    @Mapping(target = "createdAt" , source = "createdAt")
    public abstract OrderSummaryResponse toOrderSummaryResponse(OrderSummaryProjection orderSummaryProjection);

    @Mapping(target = "order", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    public abstract OrderItem toOrderItem(CreateOrderItemRequest request);

    public abstract OrderResponse toResponse(Order order);

    protected JsonNode map(AddressDto address) {
        return address == null ? null : objectMapper.valueToTree(address);
    }

    protected AddressDto map(JsonNode node) {
        return node == null ? null : objectMapper.convertValue(node, AddressDto.class);
    }

}



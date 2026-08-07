package org.ecommerce.inventoryservice.mapper;

import org.ecommerce.inventoryservice.model.entity.InventoryTransaction;
import org.ecommerce.inventoryservice.model.response.InventoryTransactionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InventoryMapper {

    @Mapping(target = "productId", expression = "java(transaction.getProduct().getId())")
    InventoryTransactionResponse toTransactionResponse(InventoryTransaction transaction);
}

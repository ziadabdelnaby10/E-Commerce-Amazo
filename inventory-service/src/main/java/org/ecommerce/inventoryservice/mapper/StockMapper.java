package org.ecommerce.inventoryservice.mapper;

import org.ecommerce.inventoryservice.model.entity.LowStockAlert;
import org.ecommerce.inventoryservice.model.entity.StockLevel;
import org.ecommerce.inventoryservice.model.response.LowStockAlertResponse;
import org.ecommerce.inventoryservice.model.response.SimpleStockLevelResponse;
import org.ecommerce.inventoryservice.model.response.StockLevelResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StockMapper {

    @Mapping(target = "productId", source = "product.id")
    StockLevelResponse toStockLevelResponse(StockLevel stockLevel);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productSku", source = "product.sku")
    LowStockAlertResponse toLowStockAlertResponse(LowStockAlert lowStockAlert);

    @Mapping(target = "productId", source = "product.id")
    StockLevelResponse toResponse(StockLevel stockLevel);

    SimpleStockLevelResponse toSimpleResponse(StockLevel stockLevel);
}

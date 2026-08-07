package org.ecommerce.inventoryservice.service;

import org.ecommerce.inventoryservice.model.entity.Product;
import org.ecommerce.inventoryservice.model.entity.StockLevel;
import org.ecommerce.inventoryservice.model.request.ProductRequest;
import org.ecommerce.inventoryservice.model.request.StockAdjustmentRequest;
import org.ecommerce.inventoryservice.model.response.LowStockAlertResponse;
import org.ecommerce.inventoryservice.model.response.SimpleStockLevelResponse;
import org.ecommerce.inventoryservice.model.response.StockLevelResponse;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface StockService {

    StockLevel saveStockLevel(StockLevel stockLevel);

    StockLevel saveEmptyStockLevel(Product product, ProductRequest request, Instant now);

    List<LowStockAlertResponse> listLowStockAlerts(boolean includeResolved);

    LowStockAlertResponse resolveLowStockAlert(Long alertId);

    StockLevelResponse getStockLevel(Long productId);

    StockLevelResponse adjustStock(Long productId, StockAdjustmentRequest request);

    StockLevel findStockLevelByProductId(Long productId);

    SimpleStockLevelResponse getSimpleStockLevel(Long productId);
}

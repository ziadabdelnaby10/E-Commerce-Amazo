package org.ecommerce.inventoryservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.inventoryservice.utils.Utils;
import org.ecommerce.inventoryservice.event.StockAdjustedEvent;
import org.ecommerce.inventoryservice.mapper.StockMapper;
import org.ecommerce.inventoryservice.model.entity.LowStockAlert;
import org.ecommerce.inventoryservice.model.entity.Product;
import org.ecommerce.inventoryservice.model.entity.ProductStatus;
import org.ecommerce.inventoryservice.model.entity.StockLevel;
import org.ecommerce.inventoryservice.model.request.ProductRequest;
import org.ecommerce.inventoryservice.model.request.StockAdjustmentRequest;
import org.ecommerce.inventoryservice.model.response.LowStockAlertResponse;
import org.ecommerce.inventoryservice.model.response.SimpleStockLevelResponse;
import org.ecommerce.inventoryservice.model.response.StockLevelResponse;
import org.ecommerce.inventoryservice.repository.LowStockAlertRepository;
import org.ecommerce.inventoryservice.repository.StockLevelRepository;
import org.ecommerce.inventoryservice.service.StockService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class StockServiceImpl implements StockService {

    private final LowStockAlertRepository lowStockAlertRepository;
    private final StockLevelRepository stockLevelRepository;
    private final StockMapper stockMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Override
    public StockLevel saveStockLevel(StockLevel stockLevel) {
        return stockLevelRepository.save(stockLevel);
    }

    @Transactional
    @Override
    public StockLevel saveEmptyStockLevel(Product product, ProductRequest request, Instant now) {
        StockLevel emptyStockLevel = StockLevel.builder()
                .product(product)
                .quantityAvailable(request.initialQuantity() == null ? 0 : request.initialQuantity())
                .quantityReserved(0)
                .quantityDamaged(0)
                .totalQuantity(request.initialQuantity() == null ? 0 : request.initialQuantity())
                .warehouseLocation(request.warehouseLocation())
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();
        return saveStockLevel(emptyStockLevel);
    }

    @Override
    public List<LowStockAlertResponse> listLowStockAlerts(boolean includeResolved) {
        List<LowStockAlert> alerts = includeResolved
                ? lowStockAlertRepository.findAllByOrderByCreatedAtDesc()
                : lowStockAlertRepository.findByResolvedAtIsNullOrderByCreatedAtDesc();
        return alerts.stream().map(stockMapper::toLowStockAlertResponse).toList();
    }

    @Transactional
    @Override
    public LowStockAlertResponse resolveLowStockAlert(Long alertId) {
        LowStockAlert alert = lowStockAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Low stock alert not found"));
        if (alert.getResolvedAt() == null) {
            alert.setResolvedAt(Instant.now());
            lowStockAlertRepository.save(alert);
        }
        return stockMapper.toLowStockAlertResponse(alert);
    }

    @Override
    public StockLevelResponse getStockLevel(Long productId) {
        return stockMapper.toStockLevelResponse(findStockLevelByProductId(productId));
    }

    @Transactional
    @Override
    public StockLevelResponse adjustStock(Long productId, StockAdjustmentRequest request) {
        StockLevel stockLevel = findStockLevelByProductId(productId);
        Product product = stockLevel.getProduct();
        int quantity = request.quantity();
        //TODO add validation on request.quantity so it won't be <= 0
//        if (quantity == 0) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock quantity must not be zero");
//        }

        int newAvailable = stockLevel.getQuantityAvailable() + quantity;
        if (newAvailable < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient available stock");
        }

        Instant now = Instant.now();
        stockLevel.setQuantityAvailable(newAvailable);
        stockLevel.setUpdatedAt(now);
        stockLevel.setVersion(Utils.nextVersion(stockLevel.getVersion()));
        ProductStatus targetStatus = resolveStatusAfterAdjustment(product.getStatus(), newAvailable);

        stockLevelRepository.save(stockLevel);

        eventPublisher.publishEvent(new StockAdjustedEvent(productId, targetStatus, request));

        ensureLowStockAlert(product, stockLevel, now);

        return stockMapper.toStockLevelResponse(stockLevel);
    }

    @Override
    public StockLevel findStockLevelByProductId(Long productId) {
        return stockLevelRepository.findByProductId(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock level not found for product ID: " + productId));
    }

    @Override
    public SimpleStockLevelResponse getSimpleStockLevel(Long productId) {
        var stocklevel = findStockLevelByProductId(productId);
        return stockMapper.toSimpleResponse(stocklevel);
    }

    private void ensureLowStockAlert(Product product, StockLevel stockLevel, Instant now) {
        if (stockLevel.getQuantityAvailable() > product.getReorderLevel()) {
            lowStockAlertRepository
                    .findFirstByProduct_IdAndResolvedAtIsNullOrderByCreatedAtDesc(product.getId())
                    .ifPresent(alert -> {
                        alert.setResolvedAt(now);
                        lowStockAlertRepository.save(alert);
                    });
            return;
        }

        lowStockAlertRepository
                .findFirstByProduct_IdAndResolvedAtIsNullOrderByCreatedAtDesc(product.getId())
                .ifPresentOrElse(alert -> {
                    alert.setCurrentQuantity(stockLevel.getQuantityAvailable());
                    alert.setReorderLevel(product.getReorderLevel());
                    lowStockAlertRepository.save(alert);
                }, () -> lowStockAlertRepository.save(LowStockAlert.builder()
                        .product(product)
                        .currentQuantity(stockLevel.getQuantityAvailable())
                        .reorderLevel(product.getReorderLevel())
                        .purchaseOrderCreated(false)
                        .createdAt(now)
                        .build()));
    }

    private ProductStatus resolveStatusAfterAdjustment(ProductStatus currentStatus, int newAvailable) {
        if (newAvailable == 0) {
            return ProductStatus.OUT_OF_STOCK;
        }
        if (currentStatus == ProductStatus.OUT_OF_STOCK) {
            return ProductStatus.ACTIVE;
        }
        return currentStatus;
    }
}

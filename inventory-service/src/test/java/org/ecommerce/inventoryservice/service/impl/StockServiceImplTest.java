package org.ecommerce.inventoryservice.service.impl;

import org.ecommerce.inventoryservice.event.StockAdjustedEvent;
import org.ecommerce.inventoryservice.mapper.StockMapper;
import org.ecommerce.inventoryservice.model.entity.LowStockAlert;
import org.ecommerce.inventoryservice.model.entity.Product;
import org.ecommerce.inventoryservice.model.entity.ProductStatus;
import org.ecommerce.inventoryservice.model.entity.StockLevel;
import org.ecommerce.inventoryservice.model.request.ProductRequest;
import org.ecommerce.inventoryservice.model.request.StockAdjustmentRequest;
import org.ecommerce.inventoryservice.model.response.LowStockAlertResponse;
import org.ecommerce.inventoryservice.model.response.StockLevelResponse;
import org.ecommerce.inventoryservice.repository.LowStockAlertRepository;
import org.ecommerce.inventoryservice.repository.StockLevelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock
    private LowStockAlertRepository lowStockAlertRepository;

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private StockMapper stockMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private StockServiceImpl stockService;

    private Product product;
    private StockLevel stockLevel;

    @BeforeEach
    void setUp() {
        product = product(1L, ProductStatus.ACTIVE, 10);
        stockLevel = stockLevel(product, 2, 2L);
    }

    @Test
    void saveEmptyStockLevel_shouldSeedNewStockUsingRequestDefaults() {
        ProductRequest request = new ProductRequest(
                "SKU-1",
                "Phone",
                "Smart phone",
                "Electronics",
                new BigDecimal("999.99"),
                new BigDecimal("700.00"),
                15L,
                10,
                50,
                null,
                "A-01"
        );
        Instant now = Instant.parse("2026-08-07T12:00:00Z");

        StockLevel created = stockService.saveEmptyStockLevel(product, request, now);

        assertThat(created.getProduct()).isEqualTo(product);
        assertThat(created.getQuantityAvailable()).isZero();
        assertThat(created.getQuantityReserved()).isZero();
        assertThat(created.getQuantityDamaged()).isZero();
        assertThat(created.getWarehouseLocation()).isEqualTo("A-01");
        assertThat(created.getCreatedAt()).isEqualTo(now);
        assertThat(created.getUpdatedAt()).isEqualTo(now);
        assertThat(created.getVersion()).isZero();
    }

    @Test
    void adjustStock_shouldRejectAdjustmentThatWouldMakeAvailableQuantityNegative() {
        when(stockLevelRepository.findByProductId(1L)).thenReturn(Optional.of(stockLevel));

        assertThatThrownBy(() -> stockService.adjustStock(1L, adjustment(-3)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Insufficient available stock")
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(stockLevelRepository, never()).save(any(StockLevel.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void adjustStock_shouldPublishOutOfStockEventAndCreateAlertWhenQuantityDropsToZero() {
        StockLevelResponse response = new StockLevelResponse(11L, 1L, 0, 0, 0, 0, "A-01", null, null, null, 3L);
        when(stockLevelRepository.findByProductId(1L)).thenReturn(Optional.of(stockLevel));
        when(lowStockAlertRepository.findFirstByProduct_IdAndResolvedAtIsNullOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());
        when(stockMapper.toStockLevelResponse(stockLevel)).thenReturn(response);

        StockLevelResponse result = stockService.adjustStock(1L, adjustment(-2));

        assertThat(result).isEqualTo(response);
        assertThat(stockLevel.getQuantityAvailable()).isZero();
        assertThat(stockLevel.getVersion()).isEqualTo(3L);
        assertThat(stockLevel.getUpdatedAt()).isNotNull();

        ArgumentCaptor<StockAdjustedEvent> eventCaptor = ArgumentCaptor.forClass(StockAdjustedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().productId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().targetStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);
        assertThat(eventCaptor.getValue().request().quantity()).isEqualTo(-2);

        ArgumentCaptor<LowStockAlert> alertCaptor = ArgumentCaptor.forClass(LowStockAlert.class);
        verify(lowStockAlertRepository).save(alertCaptor.capture());
        LowStockAlert createdAlert = alertCaptor.getValue();
        assertThat(createdAlert.getProduct()).isEqualTo(product);
        assertThat(createdAlert.getCurrentQuantity()).isZero();
        assertThat(createdAlert.getReorderLevel()).isEqualTo(10);
        assertThat(createdAlert.getPurchaseOrderCreated()).isFalse();
        assertThat(createdAlert.getResolvedAt()).isNull();
    }

    @Test
    void adjustStock_shouldResolveExistingAlertWhenStockRecoversAboveReorderLevel() {
        Product outOfStockProduct = product(1L, ProductStatus.OUT_OF_STOCK, 10);
        StockLevel lowStockLevel = stockLevel(outOfStockProduct, 8, 5L);
        LowStockAlert openAlert = LowStockAlert.builder()
                .id(90L)
                .product(outOfStockProduct)
                .currentQuantity(8)
                .reorderLevel(10)
                .createdAt(Instant.parse("2026-08-07T11:00:00Z"))
                .build();
        StockLevelResponse response = new StockLevelResponse(11L, 1L, 13, 0, 0, 13, "A-01", null, null, null, 6L);
        when(stockLevelRepository.findByProductId(1L)).thenReturn(Optional.of(lowStockLevel));
        when(lowStockAlertRepository.findFirstByProduct_IdAndResolvedAtIsNullOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(openAlert));
        when(stockMapper.toStockLevelResponse(lowStockLevel)).thenReturn(response);

        StockLevelResponse result = stockService.adjustStock(1L, adjustment(5));

        assertThat(result).isEqualTo(response);
        assertThat(openAlert.getResolvedAt()).isNotNull();
        verify(lowStockAlertRepository).save(openAlert);

        ArgumentCaptor<StockAdjustedEvent> eventCaptor = ArgumentCaptor.forClass(StockAdjustedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().targetStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void listLowStockAlerts_shouldUseUnresolvedQueryWhenResolvedAlertsAreExcluded() {
        LowStockAlert alert = LowStockAlert.builder()
                .id(22L)
                .product(product)
                .currentQuantity(2)
                .reorderLevel(10)
                .createdAt(Instant.parse("2026-08-07T11:00:00Z"))
                .build();
        LowStockAlertResponse response = new LowStockAlertResponse(22L, 1L, "SKU-1", 2, 10, null, false, null, alert.getCreatedAt(), null);
        when(lowStockAlertRepository.findByResolvedAtIsNullOrderByCreatedAtDesc()).thenReturn(List.of(alert));
        when(stockMapper.toLowStockAlertResponse(alert)).thenReturn(response);

        List<LowStockAlertResponse> result = stockService.listLowStockAlerts(false);

        assertThat(result).containsExactly(response);
        verify(lowStockAlertRepository).findByResolvedAtIsNullOrderByCreatedAtDesc();
        verify(lowStockAlertRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void resolveLowStockAlert_shouldMarkAlertAsResolvedOnlyOnce() {
        LowStockAlert alert = LowStockAlert.builder()
                .id(7L)
                .product(product)
                .currentQuantity(2)
                .reorderLevel(10)
                .createdAt(Instant.parse("2026-08-07T11:00:00Z"))
                .build();
        LowStockAlertResponse response = new LowStockAlertResponse(7L, 1L, "SKU-1", 2, 10, null, false, null, alert.getCreatedAt(), Instant.now());
        when(lowStockAlertRepository.findById(7L)).thenReturn(Optional.of(alert));
        when(stockMapper.toLowStockAlertResponse(alert)).thenReturn(response);

        LowStockAlertResponse result = stockService.resolveLowStockAlert(7L);

        assertThat(result).isEqualTo(response);
        assertThat(alert.getResolvedAt()).isNotNull();
        verify(lowStockAlertRepository).save(alert);
    }

    private StockAdjustmentRequest adjustment(int quantity) {
        return new StockAdjustmentRequest(quantity, "SALE", "ORDER-1", "ORDER", "adjustment", "system", null);
    }

    private Product product(Long id, ProductStatus status, int reorderLevel) {
        Product product = new Product();
        product.setId(id);
        product.setSku("SKU-1");
        product.setName("Phone");
        product.setStatus(status);
        product.setReorderLevel(reorderLevel);
        return product;
    }

    private StockLevel stockLevel(Product product, int quantityAvailable, Long version) {
        StockLevel stockLevel = new StockLevel();
        stockLevel.setId(11L);
        stockLevel.setProduct(product);
        stockLevel.setQuantityAvailable(quantityAvailable);
        stockLevel.setQuantityReserved(0);
        stockLevel.setQuantityDamaged(0);
        stockLevel.setWarehouseLocation("A-01");
        stockLevel.setVersion(version);
        return stockLevel;
    }
}



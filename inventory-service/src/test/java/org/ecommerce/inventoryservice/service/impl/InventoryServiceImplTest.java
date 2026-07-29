package org.ecommerce.inventoryservice.service.impl;

import org.ecommerce.inventoryservice.model.entity.Product;
import org.ecommerce.inventoryservice.model.entity.ProductStatus;
import org.ecommerce.inventoryservice.model.entity.StockLevel;
import org.ecommerce.inventoryservice.model.entity.LowStockAlert;
import org.ecommerce.inventoryservice.repository.InventoryTransactionRepository;
import org.ecommerce.inventoryservice.repository.LowStockAlertRepository;
import org.ecommerce.inventoryservice.repository.ProductRepository;
import org.ecommerce.inventoryservice.repository.StockLevelRepository;
import org.ecommerce.inventoryservice.model.request.ProductRequest;
import org.ecommerce.inventoryservice.model.request.StockAdjustmentRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

//@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

//    @Mock
//    private ProductRepository productRepository;
//
//    @Mock
//    private StockLevelRepository stockLevelRepository;
//
//    @Mock
//    private InventoryTransactionRepository inventoryTransactionRepository;
//
//    @Mock
//    private LowStockAlertRepository lowStockAlertRepository;
//
//    @InjectMocks
//    private InventoryServiceImpl inventoryService;

//    @Test
//    void createProduct_defaultsToActiveAndCreatesZeroStock() {
//        when(productRepository.existsBySku("SKU-100")).thenReturn(false);
//        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
//            Product product = invocation.getArgument(0);
//            product.setId(1L);
//            return product;
//        });
//        when(stockLevelRepository.save(any(StockLevel.class))).thenAnswer(invocation -> {
//            StockLevel stockLevel = invocation.getArgument(0);
//            stockLevel.setId(10L);
//            return stockLevel;
//        });
//
//        var response = inventoryService.createProduct(new ProductRequest(
//                "SKU-100",
//                "Demo Product",
//                "demo description",
//                "Category",
//                new BigDecimal("12.50"),
//                new BigDecimal("7.00"),
//                42L,
//                null,
//                null,
//                0,
//                "A-1-1"
//        ));
//
//        assertThat(response.id()).isEqualTo(1L);
//        assertThat(response.status()).isEqualTo(ProductStatus.ACTIVE);
//        assertThat(response.stockLevel()).isNotNull();
//        assertThat(response.stockLevel().quantityAvailable()).isZero();
//        verify(productRepository).save(any(Product.class));
//        verify(stockLevelRepository).save(any(StockLevel.class));
//    }
//
//    @Test
//    void adjustStock_rejectsNegativeResultingInventory() {
//        Product product = Product.builder()
//                .id(1L)
//                .sku("SKU-100")
//                .name("Demo Product")
//                .price(new BigDecimal("12.50"))
//                .status(ProductStatus.ACTIVE)
//                .reorderLevel(10)
//                .reorderQuantity(50)
//                .build();
//        StockLevel stockLevel = StockLevel.builder()
//                .id(11L)
//                .product(product)
//                .quantityAvailable(3)
//                .quantityReserved(0)
//                .quantityDamaged(0)
//                .build();
//
//        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//        when(stockLevelRepository.findByProductId(1L)).thenReturn(Optional.of(stockLevel));
//
//        assertThatThrownBy(() -> inventoryService.adjustStock(1L, new StockAdjustmentRequest(
//                -5,
//                "SALE",
//                "ORDER-1",
//                "ORDER",
//                "too much",
//                "system",
//                null
//        ))).isInstanceOf(ResponseStatusException.class);
//    }
//
//    @Test
//    void adjustStock_createsLowStockAlertWhenThresholdIsReached() {
//        Product product = Product.builder()
//                .id(1L)
//                .sku("SKU-100")
//                .name("Demo Product")
//                .price(new BigDecimal("12.50"))
//                .status(ProductStatus.ACTIVE)
//                .reorderLevel(10)
//                .reorderQuantity(50)
//                .version(1L)
//                .build();
//        StockLevel stockLevel = StockLevel.builder()
//                .id(11L)
//                .product(product)
//                .quantityAvailable(12)
//                .quantityReserved(0)
//                .quantityDamaged(0)
//                .version(1L)
//                .build();
//
//        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//        when(stockLevelRepository.findByProductId(1L)).thenReturn(Optional.of(stockLevel));
//        when(lowStockAlertRepository.findFirstByProduct_IdAndResolvedAtIsNullOrderByCreatedAtDesc(1L))
//                .thenReturn(Optional.empty());
//
//        inventoryService.adjustStock(1L, new StockAdjustmentRequest(
//                -3,
//                "SALE",
//                "ORDER-10",
//                "ORDER",
//                "sold",
//                "system",
//                null
//        ));
//
//        verify(lowStockAlertRepository).save(any(LowStockAlert.class));
//    }
//
//    @Test
//    void adjustStock_resolvesOpenAlertWhenStockRecovers() {
//        Product product = Product.builder()
//                .id(1L)
//                .sku("SKU-100")
//                .name("Demo Product")
//                .price(new BigDecimal("12.50"))
//                .status(ProductStatus.ACTIVE)
//                .reorderLevel(10)
//                .reorderQuantity(50)
//                .version(1L)
//                .build();
//        StockLevel stockLevel = StockLevel.builder()
//                .id(11L)
//                .product(product)
//                .quantityAvailable(8)
//                .quantityReserved(0)
//                .quantityDamaged(0)
//                .version(1L)
//                .build();
//        LowStockAlert openAlert = LowStockAlert.builder()
//                .id(50L)
//                .product(product)
//                .currentQuantity(8)
//                .reorderLevel(10)
//                .createdAt(Instant.now())
//                .build();
//
//        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//        when(stockLevelRepository.findByProductId(1L)).thenReturn(Optional.of(stockLevel));
//        when(lowStockAlertRepository.findFirstByProduct_IdAndResolvedAtIsNullOrderByCreatedAtDesc(1L))
//                .thenReturn(Optional.of(openAlert));
//
//        inventoryService.adjustStock(1L, new StockAdjustmentRequest(
//                5,
//                "PURCHASE",
//                "PO-1",
//                "PURCHASE_ORDER",
//                "restock",
//                "system",
//                null
//        ));
//
//        assertThat(openAlert.getResolvedAt()).isNotNull();
//        verify(lowStockAlertRepository).save(openAlert);
//    }
//
//    @Test
//    void listLowStockAlerts_unresolvedOnlyByDefault() {
//        Product product = Product.builder().id(1L).sku("SKU-100").build();
//        LowStockAlert alert = LowStockAlert.builder()
//                .id(22L)
//                .product(product)
//                .currentQuantity(2)
//                .reorderLevel(10)
//                .createdAt(Instant.now())
//                .build();
//
//        when(lowStockAlertRepository.findByResolvedAtIsNullOrderByCreatedAtDesc())
//                .thenReturn(List.of(alert));
//
//        var result = inventoryService.listLowStockAlerts(false);
//
//        assertThat(result).hasSize(1);
//        assertThat(result.getFirst().productSku()).isEqualTo("SKU-100");
//        verify(lowStockAlertRepository).findByResolvedAtIsNullOrderByCreatedAtDesc();
//        verify(lowStockAlertRepository, never()).findAllByOrderByCreatedAtDesc();
//    }
}


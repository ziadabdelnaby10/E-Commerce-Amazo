package org.ecommerce.inventoryservice.service.impl;

import org.ecommerce.inventoryservice.exception.AlreadyExistsException;
import org.ecommerce.inventoryservice.mapper.ProductMapper;
import org.ecommerce.inventoryservice.model.entity.Product;
import org.ecommerce.inventoryservice.model.entity.ProductStatus;
import org.ecommerce.inventoryservice.model.entity.StockLevel;
import org.ecommerce.inventoryservice.model.filter.ProductFilter;
import org.ecommerce.inventoryservice.model.request.ProductRequest;
import org.ecommerce.inventoryservice.model.request.ProductStatusUpdateRequest;
import org.ecommerce.inventoryservice.model.request.ProductUpdateRequest;
import org.ecommerce.inventoryservice.model.response.ProductResponse;
import org.ecommerce.inventoryservice.model.response.SimpleStockLevelResponse;
import org.ecommerce.inventoryservice.repository.ProductRepository;
import org.ecommerce.inventoryservice.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private StockService stockService;

    @InjectMocks
    private ProductServiceImpl productService;

    private ProductRequest createRequest;
    private Product product;
    private StockLevel stockLevel;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        createRequest = new ProductRequest(
                "SKU-1",
                "Phone",
                "Smart phone",
                "Electronics",
                new BigDecimal("999.99"),
                new BigDecimal("700.00"),
                15L,
                null,
                null,
                7,
                "A-01"
        );
        product = product(1L, "SKU-1", ProductStatus.ACTIVE, 5L);
        stockLevel = stockLevel(product, 7, "A-01", 2L);
        productResponse = response(product, 7, "A-01");
    }

    @Test
    void createProduct_shouldApplyDefaultInventorySettingsAndReturnMappedResponse() {
        Product mappedProduct = product(null, "SKU-1", ProductStatus.ACTIVE, null);
        when(productRepository.existsBySku("SKU-1")).thenReturn(false);
        when(productMapper.toEntity(createRequest)).thenReturn(mappedProduct);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(stockService.saveEmptyStockLevel(any(Product.class), eq(createRequest), any(Instant.class))).thenReturn(stockLevel);
        when(productMapper.toResponse(any(Product.class), eq(stockLevel))).thenReturn(productResponse);

        ProductResponse result = productService.createProduct(createRequest);

        assertThat(result).isEqualTo(productResponse);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();
        assertThat(savedProduct.getReorderLevel()).isEqualTo(10);
        assertThat(savedProduct.getReorderQuantity()).isEqualTo(50);
        assertThat(savedProduct.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(savedProduct.getVersion()).isZero();
        assertThat(savedProduct.getCreatedAt()).isNotNull();
        assertThat(savedProduct.getUpdatedAt()).isNotNull();
        verify(stockService).saveEmptyStockLevel(same(savedProduct), eq(createRequest), any(Instant.class));
    }

    @Test
    void createProduct_shouldRejectDuplicateSku() {
        when(productRepository.existsBySku("SKU-1")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(createRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Product SKU already exists")
                .extracting("statusCode")
                .isEqualTo(HttpStatus.CONFLICT);

        verify(productRepository, never()).save(any(Product.class));
        verify(stockService, never()).saveEmptyStockLevel(any(), any(), any());
    }

    @Test
    void updateProduct_shouldRejectSkuOwnedByAnotherProduct() {
        ProductUpdateRequest request = new ProductUpdateRequest(
                "SKU-2",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuAndIdNot("SKU-2", 1L)).thenReturn(true);

        assertThatThrownBy(() -> productService.updateProduct(1L, request))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessage("Product SKU already exists");

        verify(productMapper, never()).partialUpdate(any(), any());
        verify(stockService, never()).saveStockLevel(any());
    }

    @Test
    void updateProduct_shouldUpdateProductAndWarehouseLocationWhenSkuIsAvailable() {
        ProductUpdateRequest request = new ProductUpdateRequest(
                null,
                "Updated Phone",
                "Updated Description",
                "Electronics",
                new BigDecimal("899.99"),
                new BigDecimal("650.00"),
                null,
                25L,
                8,
                20,
                null,
                "B-04"
        );
        stockLevel.setVersion(4L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockService.findStockLevelByProductId(1L)).thenReturn(stockLevel);
        when(stockService.saveStockLevel(stockLevel)).thenReturn(stockLevel);
        when(productMapper.toResponse(product, stockLevel)).thenReturn(productResponse);

        ProductResponse result = productService.updateProduct(1L, request);

        assertThat(result).isEqualTo(productResponse);
        assertThat(product.getUpdatedAt()).isNotNull();
        assertThat(product.getVersion()).isEqualTo(6L);
        assertThat(stockLevel.getWarehouseLocation()).isEqualTo("B-04");
        assertThat(stockLevel.getUpdatedAt()).isNotNull();
        assertThat(stockLevel.getVersion()).isEqualTo(5L);
        verify(productMapper).partialUpdate(request, product);
        verify(productRepository).save(product);
        verify(stockService).saveStockLevel(stockLevel);
    }

    @Test
    void listProductsPaginated_shouldFallbackToDefaultSortWhenFilterContainsUnsupportedValues() {
        ProductFilter filter = new ProductFilter(ProductStatus.ACTIVE, null, null, null, null, null, null, "unknown", "sideways");
        PageRequest expectedPageRequest = PageRequest.of(0, 5);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product), expectedPageRequest, 1));
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        var result = productService.listProductsPaginated(filter, 0, 5);

        assertThat(result.getContent()).containsExactly(productResponse);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(5);
        var createdAtOrder = pageable.getSort().getOrderFor("createdAt");
        assertThat(createdAtOrder).isNotNull();
        assertThat(createdAtOrder.getDirection()).isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
    }

    @Test
    void updateProductStatus_shouldRejectChangingDiscontinuedProduct() {
        Product discontinuedProduct = product(9L, "SKU-9", ProductStatus.DISCONTINUED, 3L);
        when(productRepository.findById(9L)).thenReturn(Optional.of(discontinuedProduct));

        assertThatThrownBy(() -> productService.updateProductStatus(9L, new ProductStatusUpdateRequest(ProductStatus.ACTIVE)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Discontinued products cannot change status")
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void existsProductById_shouldThrowWhenProductDoesNotExist() {
        when(productRepository.existsById(42L)).thenReturn(false);

        assertThatThrownBy(() -> productService.existsProductById(42L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Product not found")
                .extracting("statusCode")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Product product(Long id, String sku, ProductStatus status, Long version) {
        Product product = new Product();
        product.setId(id);
        product.setSku(sku);
        product.setName("Phone");
        product.setDescription("Smart phone");
        product.setCategory("Electronics");
        product.setPrice(new BigDecimal("999.99"));
        product.setCost(new BigDecimal("700.00"));
        product.setSupplierId(15L);
        product.setReorderLevel(10);
        product.setReorderQuantity(50);
        product.setStatus(status);
        product.setVersion(version);
        return product;
    }

    private StockLevel stockLevel(Product product, int quantityAvailable, String warehouseLocation, Long version) {
        StockLevel stockLevel = new StockLevel();
        stockLevel.setId(10L);
        stockLevel.setProduct(product);
        stockLevel.setQuantityAvailable(quantityAvailable);
        stockLevel.setQuantityReserved(0);
        stockLevel.setQuantityDamaged(0);
        stockLevel.setWarehouseLocation(warehouseLocation);
        stockLevel.setVersion(version);
        return stockLevel;
    }

    private ProductResponse response(Product product, int quantityAvailable, String warehouseLocation) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getPrice(),
                product.getCost(),
                product.getSupplierId(),
                product.getReorderLevel(),
                product.getReorderQuantity(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getVersion(),
                new SimpleStockLevelResponse(quantityAvailable, 0, 0, quantityAvailable, warehouseLocation, null, 2L)
        );
    }
}



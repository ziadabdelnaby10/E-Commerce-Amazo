package org.ecommerce.inventoryservice.service.impl;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.inventoryservice.utils.Utils;
import org.ecommerce.inventoryservice.model.entity.Product;
import org.ecommerce.inventoryservice.model.entity.ProductStatus;
import org.ecommerce.inventoryservice.exception.AlreadyExistsException;
import org.ecommerce.inventoryservice.mapper.ProductMapper;
import org.ecommerce.inventoryservice.model.entity.StockLevel;
import org.ecommerce.inventoryservice.repository.ProductRepository;
import org.ecommerce.inventoryservice.repository.specification.ProductSpecification;
import org.ecommerce.inventoryservice.model.filter.ProductFilter;
import org.ecommerce.inventoryservice.model.request.ProductRequest;
import org.ecommerce.inventoryservice.model.request.ProductStatusUpdateRequest;
import org.ecommerce.inventoryservice.model.request.ProductUpdateRequest;
import org.ecommerce.inventoryservice.model.response.ProductResponse;
import org.ecommerce.inventoryservice.service.ProductService;
import org.ecommerce.inventoryservice.service.StockService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Set;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private static final int DEFAULT_REORDER_LEVEL = 10;
    private static final int DEFAULT_REORDER_QUANTITY = 50;
    private static final String DEFAULT_SORT_BY = "createdAt";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "sku", "name", "category", "price", "supplierId", "status", "createdAt", "updatedAt"
    );

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final StockService stockService;

    @Transactional
    @Override
    public ProductResponse createProduct(ProductRequest request) {
        checkProductExistBySku(request.sku());

        Instant now = Instant.now();
        Product product = productMapper.toEntity(request);
        product.setReorderLevel(request.reorderLevel() == null ? DEFAULT_REORDER_LEVEL : request.reorderLevel());
        product.setReorderQuantity(request.reorderQuantity() == null ? DEFAULT_REORDER_QUANTITY : request.reorderQuantity());
        product.setStatus(ProductStatus.ACTIVE);
        product.setCreatedAt(now);
        product.setUpdatedAt(now);
        product.setVersion(0L);

        var savedProduct = productRepository.save(product);
        StockLevel stockLevel = stockService.saveEmptyStockLevel(savedProduct, request, now);
        return productMapper.toResponse(savedProduct, stockLevel);
    }

    @Transactional
    @Override
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {

        Product product = findProductById(productId);

        // Reject SKU change to an already-taken SKU (owned by a different product)
        if (request.sku() != null) {
            var existProduct = productRepository.existsBySkuAndIdNot(request.sku().trim(), productId);
            if (existProduct)
                throw new AlreadyExistsException("Product SKU already exists");
        }

        productMapper.partialUpdate(request, product); // nulls are ignored, sku/name trimmed
        product.setUpdatedAt(Instant.now());
        product.setVersion(product.getVersion() == null ? 1L : product.getVersion() + 1);

        productRepository.save(product);

//      TODO add validation on request.warehouseLocation so it won't be null or empty
        StockLevel stockLevel = stockService.findStockLevelByProductId(productId);
        stockLevel.setWarehouseLocation(request.warehouseLocation());
        stockLevel.setUpdatedAt(Instant.now());
        stockLevel.setVersion(Utils.nextVersion(stockLevel.getVersion()));
        StockLevel savedStockLevel = stockService.saveStockLevel(stockLevel);

        return productMapper.toResponse(product, savedStockLevel);
    }

    @Override
    public ProductResponse getProduct(Long productId) {
        return productMapper.toResponse(findProductById(productId));
    }

    @Override
    public Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
    }

    @Override
    public Boolean existsProductById(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        return true;
    }

    @Override
    public List<ProductResponse> listProducts(ProductStatus status) {
        return (status == null ? productRepository.findAll() : productRepository.findByStatus(status))
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    public Page<ProductResponse> listProductsPaginated(ProductFilter filter, int page, int size) {
        String sortBy = resolveSortBy(filter);
        Sort.Direction direction = resolveSortDirection(filter);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Product> productPage = productRepository.findAll(ProductSpecification.fromFilter(filter), pageRequest);

        return productPage.map(productMapper::toResponse);
    }

    @Transactional
    @Override
    public ProductResponse updateProductStatus(Long productId, ProductStatusUpdateRequest request) {
        Product product = findProductById(productId);
        validateTransition(product.getStatus(), request.status());

        product.setStatus(request.status());
        product.setUpdatedAt(Instant.now());
        product.setVersion(product.getVersion() == null ? 1L : product.getVersion() + 1);

        return productMapper.toResponse(productRepository.save(product));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void checkProductExistBySku(@NotBlank String sku) {
        if (productRepository.existsBySku(sku)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Product SKU already exists");
        }
    }

    private void validateTransition(ProductStatus current, ProductStatus target) {
        if (current == target) return;
        if (current == ProductStatus.DISCONTINUED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Discontinued products cannot change status");
        }
    }

    private String resolveSortBy(ProductFilter filter) {
        if (filter == null || filter.sortBy() == null || filter.sortBy().isBlank()) {
            return DEFAULT_SORT_BY;
        }
        String sortBy = filter.sortBy().trim();
        return ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : DEFAULT_SORT_BY;
    }

    private Sort.Direction resolveSortDirection(ProductFilter filter) {
        if (filter == null || filter.sortDirection() == null || filter.sortDirection().isBlank()) {
            return Sort.Direction.DESC;
        }
        return Sort.Direction.fromOptionalString(filter.sortDirection().trim())
                .orElse(Sort.Direction.DESC);
    }
}

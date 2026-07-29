package org.ecommerce.inventoryservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ecommerce.inventoryservice.model.entity.ProductStatus;
import org.ecommerce.inventoryservice.model.filter.ProductFilter;
import org.ecommerce.inventoryservice.model.request.ProductRequest;
import org.ecommerce.inventoryservice.model.request.ProductStatusUpdateRequest;
import org.ecommerce.inventoryservice.model.request.ProductUpdateRequest;
import org.ecommerce.inventoryservice.model.request.StockAdjustmentRequest;
import org.ecommerce.inventoryservice.model.response.InventoryTransactionResponse;
import org.ecommerce.inventoryservice.model.response.ProductResponse;
import org.ecommerce.inventoryservice.model.response.StockLevelResponse;
import org.ecommerce.inventoryservice.service.InventoryService;
import org.ecommerce.inventoryservice.service.ProductService;
import org.ecommerce.inventoryservice.service.StockService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final ProductService productService;
    private final StockService stockService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> list(@RequestParam(required = false) ProductStatus status) {
        return ResponseEntity.ok(productService.listProducts(status));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> get(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProduct(productId));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> update(@PathVariable Long productId, @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(productService.updateProduct(productId, request));
    }

    @PatchMapping("/{productId}/status")
    public ResponseEntity<ProductResponse> updateStatus(@PathVariable Long productId, @Valid @RequestBody ProductStatusUpdateRequest request) {
        return ResponseEntity.ok(productService.updateProductStatus(productId, request));
    }

    @GetMapping("/{productId}/stock")
    public ResponseEntity<StockLevelResponse> stock(@PathVariable Long productId) {
        return ResponseEntity.ok(stockService.getStockLevel(productId));
    }

    @PostMapping("/{productId}/stock/adjust")
    public ResponseEntity<StockLevelResponse> adjustStock(@PathVariable Long productId, @Valid @RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.ok(stockService.adjustStock(productId, request));
    }

    @GetMapping("/{productId}/transactions")
    public ResponseEntity<Page<InventoryTransactionResponse>> transactions(
            @PathVariable Long productId,
            @PageableDefault(size = 20, direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(inventoryService.getTransactionHistory(productId, pageable));
    }
}


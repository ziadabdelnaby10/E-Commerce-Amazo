package org.ecommerce.inventoryservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.inventoryservice.mapper.InventoryMapper;
import org.ecommerce.inventoryservice.model.entity.InventoryTransaction;
import org.ecommerce.inventoryservice.model.entity.Product;
import org.ecommerce.inventoryservice.model.request.StockAdjustmentRequest;
import org.ecommerce.inventoryservice.model.response.InventoryTransactionResponse;
import org.ecommerce.inventoryservice.repository.InventoryTransactionRepository;
import org.ecommerce.inventoryservice.repository.ProductRepository;
import org.ecommerce.inventoryservice.service.InventoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final InventoryMapper inventoryMapper;

    @Transactional
    @Override
    public InventoryTransaction addTransaction(Long productId, StockAdjustmentRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        InventoryTransaction transaction = buildTransaction(product, request, Instant.now());
        return inventoryTransactionRepository.save(transaction);
    }

    @Override
    public Page<InventoryTransactionResponse> getTransactionHistory(Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        return inventoryTransactionRepository.findByProduct_IdOrderByCreatedAtDesc(productId, pageable)
                .map(inventoryMapper::toTransactionResponse);
    }


    private InventoryTransaction buildTransaction(Product product, StockAdjustmentRequest request, Instant now) {
        return InventoryTransaction.builder()
                .product(product)
                .transactionType(request.transactionType() == null || request.transactionType().isBlank()
                        ? "STOCK_ADJUSTMENT"
                        : request.transactionType().trim())
                .quantity(request.quantity())
                .referenceId(request.referenceId())
                .referenceType(request.referenceType())
                .reason(request.reason())
                .createdBy(request.createdBy())
                .notes(request.notes())
                .createdAt(now)
                .build();
    }

}


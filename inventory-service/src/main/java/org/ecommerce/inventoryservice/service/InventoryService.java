package org.ecommerce.inventoryservice.service;

import org.ecommerce.inventoryservice.model.entity.InventoryTransaction;
import org.ecommerce.inventoryservice.model.request.StockAdjustmentRequest;
import org.ecommerce.inventoryservice.model.response.InventoryTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryService {

    InventoryTransaction addTransaction(Long productId, StockAdjustmentRequest request);

    Page<InventoryTransactionResponse> getTransactionHistory(Long productId, Pageable pageable);


}


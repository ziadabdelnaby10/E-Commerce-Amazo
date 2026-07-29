package org.ecommerce.inventoryservice.event;

import lombok.RequiredArgsConstructor;
import org.ecommerce.inventoryservice.service.InventoryService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class StockAdjustedInventoryTransactionListener {

    private final InventoryService inventoryService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onStockAdjusted(StockAdjustedEvent event) {
        inventoryService.addTransaction(event.productId(), event.request());
    }
}


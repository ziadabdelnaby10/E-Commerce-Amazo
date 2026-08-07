package org.ecommerce.inventoryservice.event;

import lombok.RequiredArgsConstructor;
import org.ecommerce.inventoryservice.model.request.ProductStatusUpdateRequest;
import org.ecommerce.inventoryservice.service.ProductService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class StockAdjustedProductStatusListener {

    private final ProductService productService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onStockAdjusted(StockAdjustedEvent event) {
        productService.updateProductStatus(
                event.productId(),
                new ProductStatusUpdateRequest(event.targetStatus())
        );
    }
}


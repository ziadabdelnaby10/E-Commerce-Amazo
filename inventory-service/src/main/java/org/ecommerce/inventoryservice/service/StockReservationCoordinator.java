package org.ecommerce.inventoryservice.service;

import lombok.RequiredArgsConstructor;
import org.ecommerce.inventoryservice.lock.RedisDistributedLock;
import org.ecommerce.inventoryservice.model.request.ReleaseInventoryRequest;
import org.ecommerce.inventoryservice.model.request.ReserveInventoryItemRequest;
import org.ecommerce.inventoryservice.model.request.ReserveInventoryRequest;
import org.ecommerce.inventoryservice.model.response.ReserveInventoryResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serialises reservation and release of the same products across service instances.
 *
 * <p>This sits deliberately <em>outside</em> {@code StockServiceImpl}: the lock has to be held for
 * the whole database transaction, and Spring starts the transaction when the {@code @Transactional}
 * proxy is entered. Acquiring the lock inside that method would release it before the commit,
 * leaving the oversell window open. Locking here means the ordering is:
 * acquire lock -> begin transaction -> commit -> release lock.</p>
 */
@Service
@RequiredArgsConstructor
public class StockReservationCoordinator {

    private final RedisDistributedLock distributedLock;
    private final StockService stockService;

    public ReserveInventoryResponse reserveInventory(ReserveInventoryRequest request) {
        return distributedLock.executeLocked(
                lockKeys(request.items()),
                () -> stockService.reserveInventory(request));
    }

    public void releaseInventory(ReleaseInventoryRequest request) {
        distributedLock.executeLocked(lockKeys(request.items()), () -> {
            stockService.releaseInventory(request);
            return null;
        });
    }

    private List<String> lockKeys(List<ReserveInventoryItemRequest> items) {
        return items.stream()
                .map(item -> "product:" + item.productId())
                .distinct()
                .toList();
    }
}


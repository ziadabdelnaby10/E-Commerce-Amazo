package org.ecommerce.inventoryservice.repository;

import org.ecommerce.inventoryservice.model.entity.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    Page<InventoryTransaction> findByProduct_IdOrderByCreatedAtDesc(Long productId, Pageable pageable);
}


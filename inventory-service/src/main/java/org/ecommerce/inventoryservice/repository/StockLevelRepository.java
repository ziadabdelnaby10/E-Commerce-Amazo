package org.ecommerce.inventoryservice.repository;

import org.ecommerce.inventoryservice.model.entity.StockLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockLevelRepository extends JpaRepository<StockLevel, Long> {
    Optional<StockLevel> findByProductId(Long productId);
}


package org.ecommerce.inventoryservice.repository;

import org.ecommerce.inventoryservice.model.entity.LowStockAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LowStockAlertRepository extends JpaRepository<LowStockAlert, Long> {
    boolean existsByProduct_IdAndResolvedAtIsNull(Long productId);

    List<LowStockAlert> findByResolvedAtIsNullOrderByCreatedAtDesc();

    List<LowStockAlert> findAllByOrderByCreatedAtDesc();

    Optional<LowStockAlert> findByIdAndResolvedAtIsNull(Long id);

    Optional<LowStockAlert> findFirstByProduct_IdAndResolvedAtIsNullOrderByCreatedAtDesc(Long productId);
}


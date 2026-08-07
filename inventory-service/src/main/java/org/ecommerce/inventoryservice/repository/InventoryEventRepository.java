package org.ecommerce.inventoryservice.repository;

import org.ecommerce.inventoryservice.model.entity.InventoryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryEventRepository extends JpaRepository<InventoryEvent, Long> {
}


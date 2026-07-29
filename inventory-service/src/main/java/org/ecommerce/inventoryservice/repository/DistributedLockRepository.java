package org.ecommerce.inventoryservice.repository;

import org.ecommerce.inventoryservice.model.entity.DistributedLock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistributedLockRepository extends JpaRepository<DistributedLock, Long> {
}


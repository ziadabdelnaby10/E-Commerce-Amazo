package org.ecommerce.customerservice.repository;

import org.ecommerce.customerservice.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<Permission> findByNameIgnoreCase(String name);
}


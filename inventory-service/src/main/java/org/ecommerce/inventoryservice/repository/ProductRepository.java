package org.ecommerce.inventoryservice.repository;

import org.ecommerce.inventoryservice.model.entity.Product;
import org.ecommerce.inventoryservice.model.entity.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findBySku(String sku);

    @Query("select (count(p) > 0) from Product p where p.sku = ?1 and p.id <> ?2")
    boolean existsBySkuAndIdNot(String sku, Long productId);

    @Query("select (count(p) > 0) from Product p where p.sku = ?1")
    boolean existsBySku(String sku);

    List<Product> findByStatus(ProductStatus status);
}


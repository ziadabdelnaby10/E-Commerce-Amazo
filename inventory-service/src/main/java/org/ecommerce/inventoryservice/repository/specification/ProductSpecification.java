package org.ecommerce.inventoryservice.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.ecommerce.inventoryservice.model.entity.Product;
import org.ecommerce.inventoryservice.model.filter.ProductFilter;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ProductSpecification {

    private ProductSpecification() {
    }

    public static Specification<Product> fromFilter(ProductFilter filter) {
        return (root, query, criteriaBuilder) -> {
            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (filter.status() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.status()));
            }

            if (hasText(filter.sku())) {
                predicates.add(criteriaBuilder.equal(root.get("sku"), filter.sku().trim()));
            }

            if (hasText(filter.name())) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + filter.name().trim().toLowerCase() + "%"
                ));
            }

            if (hasText(filter.category())) {
                predicates.add(criteriaBuilder.equal(root.get("category"), filter.category().trim()));
            }

            if (filter.supplierId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("supplierId"), filter.supplierId()));
            }

            if (filter.minPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), filter.minPrice()));
            }

            if (filter.maxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), filter.maxPrice()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}


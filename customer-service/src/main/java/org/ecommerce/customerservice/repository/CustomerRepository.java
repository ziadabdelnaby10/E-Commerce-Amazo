package org.ecommerce.customerservice.repository;

import org.ecommerce.customerservice.entity.Customer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

	Page<Customer> findByDeletedAtIsNull(Pageable pageable);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByIdAndDeletedAtIsNull(UUID id);

	Optional<Customer> findByIdAndDeletedAtIsNull(UUID id);

	@EntityGraph(attributePaths = {"roles", "roles.permissions"})
	Optional<Customer> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);
}

package org.ecommerce.customerservice.repository;

import org.ecommerce.customerservice.entity.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomerRepository extends MongoRepository<Customer, String> {
}

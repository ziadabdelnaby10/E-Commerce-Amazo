package org.ecommerce.customerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

/**
 * Entry point for the customer-service application.
 *
 * <p>This service owns customer registration, profile management, authentication,
 * JWT issuance, refresh-token rotation, RBAC metadata, and customer lookups used
 * by other services such as order-service.</p>
 *
 * <p>Spring Data web support is configured to serialize paged responses via DTOs,
 * which keeps pagination output stable and avoids exposing framework internals.</p>
 */
@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class CustomerServiceApplication {

    /**
     * Starts the customer-service Spring Boot application.
     *
     * @param args standard Spring Boot startup arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }

}

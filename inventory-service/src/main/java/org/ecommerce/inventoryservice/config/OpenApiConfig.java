package org.ecommerce.inventoryservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 metadata configuration for the Inventory Service.
 *
 * <p>Exposes API documentation at:
 * <ul>
 *     <li>Swagger UI: {@code /swagger-ui.html}</li>
 *     <li>OpenAPI JSON: {@code /v3/api-docs}</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI inventoryServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Inventory Service API")
                        .description("""
                                REST API for managing products, stock levels, inventory transactions and low-stock alerts.
                                
                                **Key features:**
                                - Product lifecycle management (create, update, status transitions)
                                - Stock adjustments with domain event propagation
                                - Low-stock alert tracking and resolution
                                - Paginated transaction history
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("E-Commerce Platform Team")
                                .email("team@ecommerce.org"))
                        .license(new License()
                                .name("Internal Use Only")))
                .servers(List.of(
                        new Server().url("http://localhost:9002").description("Local development"),
                        new Server().url("http://gateway:9080/inventory").description("Via API Gateway")
                ));
    }
}


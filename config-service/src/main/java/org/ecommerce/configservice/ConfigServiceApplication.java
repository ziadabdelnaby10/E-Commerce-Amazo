package org.ecommerce.configservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Bootstrap class for the centralized Spring Cloud Config Server.
 *
 * <p>This service is responsible for serving configuration files to the rest of the
 * microservices at startup time and during refresh operations. In this project the
 * config server runs in {@code native} mode, which means configuration is loaded
 * from files packaged under {@code classpath:/config} rather than from Git.</p>
 *
 * <p>Other services reference this application through
 * {@code spring.config.import=configserver:...} so they can externalize ports,
 * security settings, Kafka settings, database settings, and service-specific overrides.</p>
 */
@EnableConfigServer
@SpringBootApplication
public class ConfigServiceApplication {

    /**
     * Starts the config server application.
     *
     * @param args standard Spring Boot startup arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication.class, args);
    }

}

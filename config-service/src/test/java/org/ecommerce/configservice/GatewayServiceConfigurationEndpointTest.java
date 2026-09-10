package org.ecommerce.configservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class GatewayServiceConfigurationEndpointTest {

    private static final Pattern ROUTE_ID_KEY = Pattern.compile("spring\\.cloud\\.gateway\\.server\\.webflux\\.routes\\[\\d+].id");
    private static final Pattern ROUTE_FILTER_KEY = Pattern.compile("spring\\.cloud\\.gateway\\.server\\.webflux\\.routes\\[\\d+].filters\\[\\d+].*");

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void shouldServeGatewayRoutesAndSecurityConfiguration() {
        Map<String, String> properties = readGatewayServiceProperties();

        assertThat(extractValues(properties, ROUTE_ID_KEY))
                .containsExactlyInAnyOrder(
                        "customer-auth-service",
                        "inventory-service",
                        "order-service",
                        "payment-service",
                        "customer-service",
                        "notification-service"
                );
        assertThat(properties)
                .containsEntry("server.port", "9080")
                .containsEntry("spring.cloud.gateway.server.webflux.routes[0].uri", "lb://customer-service")
                .containsEntry("spring.cloud.gateway.server.webflux.routes[1].uri", "lb://inventory-service")
                .containsEntry("spring.cloud.gateway.server.webflux.routes[2].uri", "lb://order-service")
                .containsEntry("spring.cloud.gateway.server.webflux.routes[3].uri", "lb://payment-service")
                .containsEntry("spring.cloud.gateway.server.webflux.routes[4].uri", "lb://customer-service")
                .containsEntry("spring.cloud.gateway.server.webflux.routes[5].uri", "lb://notification-service")
                .containsEntry("application.security.enabled", "true")
                .containsEntry("application.security.jwt.secret", "I_gu6p0FVNHP_Hhjt0cHXF-eFwYGB8ph1xwUOKaBYljDfsXnHpfYAYgaRC8msRTc")
                .containsEntry("application.security.jwt.issuer", "ecommerce-platform")
                .containsEntry("management.endpoints.web.exposure.include", "health,info,prometheus,gateway");
    }

    @Test
    void shouldServeBothBaseAndWildcardPathsForPublicGatewayRoutes() {
        Map<String, String> properties = readGatewayServiceProperties();

        assertThat(properties)
                .containsEntry("spring.cloud.gateway.server.webflux.routes[0].predicates[0]", "Path=/api/v1/auth,/api/v1/auth/**")
                .containsEntry("spring.cloud.gateway.server.webflux.routes[4].predicates[0]", "Path=/api/v1/customers,/api/v1/customers/**")
                .containsEntry("spring.cloud.gateway.server.webflux.routes[1].predicates[0]", "Path=/api/v1/inventory/**")
                .containsEntry("spring.cloud.gateway.server.webflux.routes[2].predicates[0]", "Path=/api/v1/orders/**")
                .containsEntry("spring.cloud.gateway.server.webflux.routes[3].predicates[0]", "Path=/api/v1/payments/**")
                .containsEntry("spring.cloud.gateway.server.webflux.routes[5].predicates[0]", "Path=/api/v1/notifications/**");
    }

    @Test
    void shouldMergeSharedApplicationDefaultsIntoGatewayServiceConfiguration() {
        Map<String, String> properties = readGatewayServiceProperties();

        assertThat(properties)
                .containsEntry("eureka.client.service-url.defaultZone", "http://localhost:8761/eureka")
                .containsEntry("eureka.instance.hostname", "localhost")
                .containsEntry("spring.cloud.config.override-system-properties", "false");
    }

    @Test
    void shouldNotExposeCommentedOutGatewayFiltersAsActiveConfiguration() {
        Map<String, String> properties = readGatewayServiceProperties();

        assertThat(extractValues(properties, ROUTE_FILTER_KEY)).isEmpty();
    }

    private Map<String, String> readGatewayServiceProperties() {
        JsonNode body = readEnvironment("gateway-service", "default");
        assertThat(body.path("name").asText()).isEqualTo("gateway-service");

        Map<String, String> properties = new LinkedHashMap<>();
        for (JsonNode propertySource : body.path("propertySources")) {
            JsonNode source = propertySource.path("source");
            source.fields().forEachRemaining(entry -> properties.putIfAbsent(entry.getKey(), entry.getValue().asText()));
        }
        return properties;
    }

    private JsonNode readEnvironment(String applicationName, String profile) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/" + applicationName + "/" + profile))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            return objectMapper.readTree(response.body());
        } catch (IOException | InterruptedException exception) {
            throw new RuntimeException(exception);
        }
    }

    private Set<String> extractValues(Map<String, String> properties, Pattern keyPattern) {
        return properties.entrySet().stream()
                .filter(entry -> keyPattern.matcher(entry.getKey()).matches())
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
    }
}


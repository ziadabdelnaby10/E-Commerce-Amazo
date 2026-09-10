package org.ecommerce.customerservice.e2e;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ecommerce.customerservice.TestDataFactory;
import org.ecommerce.customerservice.repository.CustomerRepository;
import org.ecommerce.customerservice.request.CustomerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.servlet.context-path=/api",
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class CustomerServiceE2ETest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("customer_test")
            .withUsername("postgres")
            .withPassword("rootroot");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private JwtEncoder jwtEncoder;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private String baseUrl;

    private String adminToken;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
        baseUrl = "http://localhost:" + port + "/api/v1/customers";
        adminToken = adminToken();
    }

    @Test
    void shouldCreateAndFetchCustomer() {
        CustomerRequest request = TestDataFactory.request();

        HttpResponse<String> createResponse = sendJsonRequest("POST", baseUrl, request);

        assertThat(createResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(createResponse.body()).isNotBlank();

        Map<String, Object> createBody = readJson(createResponse.body());
        UUID customerId = UUID.fromString((String) createBody.get("data"));
        HttpResponse<String> getResponse = sendRequest("GET", baseUrl + "/" + customerId, null);

        assertThat(getResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
        Map<String, Object> body = readJson(getResponse.body());
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertThat(data.get("firstName")).isEqualTo("Ziad");
        assertThat(data.get("email")).isEqualTo("ziad@example.com");
    }

    @Test
    void shouldListCreatedCustomers() {
        sendJsonRequest("POST", baseUrl, TestDataFactory.request());

        HttpResponse<String> listResponse = sendRequest("GET", baseUrl, null);

        assertThat(listResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
        Map<String, Object> listBody = readJson(listResponse.body());
        assertThat(listBody).containsKeys("statusCode", "time", "data");
        assertThat(listResponse.body()).contains("Ziad").contains("ziad@example.com");
    }

    @Test
    void shouldUpdateUserProfile() {
        Map<String, Object> createBody = readJson(sendJsonRequest("POST", baseUrl, TestDataFactory.request()).body());
        UUID customerId = UUID.fromString((String) createBody.get("data"));
        CustomerRequest updateRequest = new CustomerRequest("Updated", "User", "updated@example.com", "+201111111111", "NewPassword123");

        HttpResponse<String> updateResponse = sendJsonRequest("PUT", baseUrl + "/" + customerId, updateRequest);

        assertThat(updateResponse.statusCode()).isEqualTo(HttpStatus.OK.value());

        var savedCustomer = customerRepository.findById(customerId).orElseThrow();
        assertThat(savedCustomer.getFirstName()).isEqualTo("Updated");
        assertThat(savedCustomer.getLastName()).isEqualTo("User");
        assertThat(savedCustomer.getEmail()).isEqualTo("updated@example.com");
        assertThat(savedCustomer.getPhoneNumber()).isEqualTo("+201111111111");
    }

    @Test
    void shouldReturnBadRequestForInvalidPayload() {
        Map<String, Object> invalidRequest = Map.of(
                "lastName", "Hassan",
                "email", "invalid-email",
                "password", ""
        );

        HttpResponse<String> response = sendJsonRequest("POST", baseUrl, invalidRequest);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        Map<String, Object> errorBody = readJson(response.body());
        assertThat(errorBody.get("errorCode")).isEqualTo(400);
        assertThat((String) errorBody.get("errorDescription")).contains("firstName").contains("Email should be valid");
    }

    @Test
    void shouldReturnBadRequestForInvalidUuid() {
        HttpResponse<String> response = sendRequest("GET", baseUrl + "/missing", null);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        Map<String, Object> errorBody = readJson(response.body());
        assertThat(errorBody.get("errorCode")).isEqualTo(400);
        assertThat(errorBody.get("errorDescription")).isEqualTo("Invalid customer id format");
    }

    @Test
    void shouldDeleteCustomer() {
        Map<String, Object> createBody = readJson(sendJsonRequest("POST", baseUrl, TestDataFactory.request()).body());
        UUID customerId = UUID.fromString((String) createBody.get("data"));

        HttpResponse<String> deleteResponse = sendRequest("DELETE", baseUrl + "/" + customerId, null);

        assertThat(deleteResponse.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
        assertThat(customerRepository.findById(customerId).orElseThrow().getDeletedAt()).isNotNull();
    }

    private HttpResponse<String> sendJsonRequest(String method, String url, Object body) {
        try {
            return sendRequest(method, url, objectMapper.writeValueAsString(body));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private HttpResponse<String> sendRequest(String method, String url, String body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + adminToken);

            if (body == null) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.method(method, HttpRequest.BodyPublishers.ofString(body));
            }

            return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /** Builds a token signed with the same key the application uses in the test profile. */
    private String adminToken() {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("ecommerce-platform")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(1, ChronoUnit.HOURS))
                .subject("admin@example.com")
                .claim("userId", UUID.randomUUID().toString())
                .claim("roles", List.of("ADMIN"))
                .claim("permissions", List.of("VIEW_USERS", "MODIFY_USER", "DELETE_USER"))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private Map<String, Object> readJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}




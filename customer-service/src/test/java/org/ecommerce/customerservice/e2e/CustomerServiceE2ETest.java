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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

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

    private static final int MONGO_PORT = 27017;

    @Container
    static GenericContainer<?> mongoContainer = new GenericContainer<>(DockerImageName.parse("mongo:7.0.3"))
            .withExposedPorts(MONGO_PORT);

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        // Support both key namespaces so tests remain stable across Boot property model changes.
        registry.add("spring.mongodb.uri", CustomerServiceE2ETest::mongoUri);
        registry.add("spring.data.mongodb.uri", CustomerServiceE2ETest::mongoUri);
    }

    private static String mongoUri() {
        return "mongodb://" + mongoContainer.getHost() + ":" + mongoContainer.getMappedPort(MONGO_PORT) + "/customer-test";
    }

    @LocalServerPort
    private int port;

    @Autowired
    private CustomerRepository customerRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private String baseUrl;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
        baseUrl = "http://localhost:" + port + "/api/v1/customers";
    }

    @Test
    void shouldCreateAndFetchCustomer() {
        CustomerRequest request = TestDataFactory.request();

        HttpResponse<String> createResponse = sendJsonRequest("POST", baseUrl, request);

        assertThat(createResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(createResponse.body()).isNotBlank();

        HttpResponse<String> getResponse = sendRequest("GET", baseUrl + "/" + createResponse.body(), null);

        assertThat(getResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
        Map<String, Object> body = readJson(getResponse.body());
        assertThat(body.get("firstName")).isEqualTo("Ziad");
        assertThat(body.get("email")).isEqualTo("ziad@example.com");
    }

    @Test
    void shouldListCreatedCustomers() {
        sendJsonRequest("POST", baseUrl, TestDataFactory.request());

        HttpResponse<String> listResponse = sendRequest("GET", baseUrl, null);

        assertThat(listResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(listResponse.body()).contains("Ziad").contains("ziad@example.com");
    }

    @Test
    void shouldPreserveAddressWhenUpdateRequestHasNullAddress() {
        String customerId = sendJsonRequest("POST", baseUrl, TestDataFactory.request()).body();
        CustomerRequest updateRequest = new CustomerRequest("Updated", "User", "updated@example.com", null);

        HttpResponse<String> updateResponse = sendJsonRequest("PUT", baseUrl + "/" + customerId, updateRequest);

        assertThat(updateResponse.statusCode()).isEqualTo(HttpStatus.OK.value());

        var savedCustomer = customerRepository.findById(customerId).orElseThrow();
        assertThat(savedCustomer.getFirstName()).isEqualTo("Updated");
        assertThat(savedCustomer.getLastName()).isEqualTo("User");
        assertThat(savedCustomer.getEmail()).isEqualTo("updated@example.com");
        assertThat(savedCustomer.getAddress()).usingRecursiveComparison().isEqualTo(TestDataFactory.address());
    }

    @Test
    void shouldReturnBadRequestForInvalidPayload() {
        Map<String, Object> invalidRequest = Map.of(
                "lastName", "Hassan",
                "email", "invalid-email"
        );

        HttpResponse<String> response = sendJsonRequest("POST", baseUrl, invalidRequest);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.body()).contains("firstName").contains("Email should be valid");
    }

    @Test
    void shouldReturnNotFoundForUnknownCustomer() {
        HttpResponse<String> response = sendRequest("GET", baseUrl + "/missing", null);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.body()).contains("Customer not found with id: missing");
    }

    @Test
    void shouldDeleteCustomer() {
        String customerId = sendJsonRequest("POST", baseUrl, TestDataFactory.request()).body();

        HttpResponse<String> deleteResponse = sendRequest("DELETE", baseUrl + "/" + customerId, null);

        assertThat(deleteResponse.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
        assertThat(customerRepository.existsById(customerId)).isFalse();
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
                    .header("Content-Type", "application/json");

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

    private Map<String, Object> readJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}




package org.ecommerce.notificationservice.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.notificationservice.application.port.out.CustomerContactPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceClient implements CustomerContactPort {

    private final RestClient restClient;

    @Value("${application.config.customer-url:http://localhost:9001/api/v1/customers}")
    private String customerBaseUrl;

    @Override
    public Optional<String> resolveEmailByUserId(Long userId) {
        try {
            CustomerLookupResponse response = restClient.get()
                    .uri(customerBaseUrl + "/" + userId)
                    .retrieve()
                    .body(CustomerLookupResponse.class);
            return response == null ? Optional.empty() : Optional.ofNullable(response.email());
        } catch (Exception ex) {
            log.warn("Failed to resolve customer email for userId={}: {}", userId, ex.getMessage());
            return Optional.empty();
        }
    }

    private record CustomerLookupResponse(String id, String firstName, String lastName, String email) {
    }
}



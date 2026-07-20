package org.ecommerce.customerservice.response;

import java.util.Map;

public record ErrorResponse(
        Map<String, String> errors
) {
}

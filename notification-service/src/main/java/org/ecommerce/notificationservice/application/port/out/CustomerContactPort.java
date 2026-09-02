package org.ecommerce.notificationservice.application.port.out;

import java.util.Optional;

public interface CustomerContactPort {
    Optional<String> resolveEmailByUserId(Long userId);
}


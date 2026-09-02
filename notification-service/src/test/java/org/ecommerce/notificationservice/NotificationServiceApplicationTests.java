package org.ecommerce.notificationservice;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationServiceApplicationTests {

    @Test
    void appClassIsLoadable() {
        assertThat(NotificationServiceApplication.class).isNotNull();
    }

}

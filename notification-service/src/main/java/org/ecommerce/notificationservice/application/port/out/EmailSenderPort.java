package org.ecommerce.notificationservice.application.port.out;

import org.ecommerce.notificationservice.application.usecase.model.SendEmailCommand;

public interface EmailSenderPort {
    void send(SendEmailCommand command);
}


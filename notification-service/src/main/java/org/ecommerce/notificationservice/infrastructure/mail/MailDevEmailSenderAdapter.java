package org.ecommerce.notificationservice.infrastructure.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.ecommerce.notificationservice.application.port.out.EmailSenderPort;
import org.ecommerce.notificationservice.application.usecase.model.SendEmailCommand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MailDevEmailSenderAdapter implements EmailSenderPort {

    private final JavaMailSender mailSender;

    @Value("${application.config.from-address:no-reply@ecommerce.local}")
    private String fromAddress;

    @Override
    public void send(SendEmailCommand command) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(command.to());
            helper.setSubject(command.subject() == null ? "Notification" : command.subject());
            helper.setText(command.body(), command.html());
            mailSender.send(mimeMessage);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to send email to " + command.to(), ex);
        }
    }
}


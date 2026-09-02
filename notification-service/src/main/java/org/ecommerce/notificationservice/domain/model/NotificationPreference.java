package org.ecommerce.notificationservice.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_preferences_seq_gen")
    @SequenceGenerator(name = "notification_preferences_seq_gen", sequenceName = "notification_preferences_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "email_on_order_created", nullable = false)
    private boolean emailOnOrderCreated;

    @Column(name = "email_on_order_shipped", nullable = false)
    private boolean emailOnOrderShipped;

    @Column(name = "email_on_order_delivered", nullable = false)
    private boolean emailOnOrderDelivered;

    @Column(name = "email_on_payment_success", nullable = false)
    private boolean emailOnPaymentSuccess;

    @Column(name = "email_on_payment_failed", nullable = false)
    private boolean emailOnPaymentFailed;

    @Column(name = "email_on_inventory_alert", nullable = false)
    private boolean emailOnInventoryAlert;

    @Column(name = "sms_on_order_shipped", nullable = false)
    private boolean smsOnOrderShipped;

    @Column(name = "sms_on_payment_failed", nullable = false)
    private boolean smsOnPaymentFailed;

    @Column(name = "push_on_order_update", nullable = false)
    private boolean pushOnOrderUpdate;

    @Column(name = "push_on_payment_update", nullable = false)
    private boolean pushOnPaymentUpdate;

    @Column(name = "unsubscribed_from_marketing", nullable = false)
    private boolean unsubscribedFromMarketing;

    @Column(name = "unsubscribed_from_all", nullable = false)
    private boolean unsubscribedFromAll;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}


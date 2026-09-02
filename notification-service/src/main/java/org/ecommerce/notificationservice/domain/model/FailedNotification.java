package org.ecommerce.notificationservice.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "failed_notifications")
@Getter
@Setter
@NoArgsConstructor
public class FailedNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "failed_notifications_seq_gen")
    @SequenceGenerator(name = "failed_notifications_seq_gen", sequenceName = "failed_notifications_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "error_stacktrace")
    private String errorStacktrace;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "next_retry_time")
    private Instant nextRetryTime;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}


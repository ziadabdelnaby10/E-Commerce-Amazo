package org.ecommerce.inventoryservice.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "distributed_locks")
public class DistributedLock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ColumnDefault("nextval('distributed_locks_id_seq')")
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "resource_name", nullable = false)
    private String resourceName;

    @Size(max = 100)
    @NotNull
    @Column(name = "locked_by", nullable = false, length = 100)
    private String lockedBy;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "acquired_at")
    private Instant acquiredAt;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Size(max = 50)
    @Column(name = "version", length = 50)
    private String version;

}
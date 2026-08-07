package org.ecommerce.inventoryservice.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "low_stock_alerts")
public class LowStockAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ColumnDefault("nextval('low_stock_alerts_id_seq')")
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull
    @Column(name = "current_quantity", nullable = false)
    private Integer currentQuantity;

    @NotNull
    @Column(name = "reorder_level", nullable = false)
    private Integer reorderLevel;

    @Column(name = "alert_sent_at")
    private Instant alertSentAt;

    @ColumnDefault("false")
    @Column(name = "purchase_order_created")
    private Boolean purchaseOrderCreated;

    @Column(name = "purchase_order_id")
    private Long purchaseOrderId;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

}
package org.ecommerce.inventoryservice.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@Table(name = "stock_levels")
public class StockLevel {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stock_levels_id_gen")
    @SequenceGenerator(name = "stock_levels_id_gen", sequenceName = "stock_levels_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "quantity_available", nullable = false)
    private Integer quantityAvailable;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "quantity_reserved", nullable = false)
    private Integer quantityReserved;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "quantity_damaged", nullable = false)
    private Integer quantityDamaged;

    @ColumnDefault("((quantity_available + quantity_reserved) + quantity_damaged)")
    @Column(name = "total_quantity", insertable = false, updatable = false)
    private Integer totalQuantity;

    @Size(max = 100)
    @Column(name = "warehouse_location", length = 100)
    private String warehouseLocation;

    @Column(name = "last_counted_at")
    private Instant lastCountedAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;

    @ColumnDefault("0")
    @Column(name = "version")
    private Long version;

}
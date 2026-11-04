package com.example.orders.adapters.outbound.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItemJpaEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false) private OrderJpaEntity order;
    @Column(nullable = false) private String sku;
    @Column(nullable = false) private int quantity;
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2) private BigDecimal unitPrice;
    @Column(nullable = false, length = 3) private String currency;

    protected OrderItemJpaEntity() {}
    public OrderItemJpaEntity(UUID id, String sku, int quantity, BigDecimal unitPrice, String currency) {
        this.id = id; this.sku = sku; this.quantity = quantity; this.unitPrice = unitPrice; this.currency = currency;
    }
    void setOrder(OrderJpaEntity order) { this.order = order; }
    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public String getCurrency() { return currency; }
}

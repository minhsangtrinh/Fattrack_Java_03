package com.example.orders.adapters.outbound.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderJpaEntity {
    @Id private UUID id;
    @Column(name = "customer_id", nullable = false) private String customerId;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal total;
    @Column(nullable = false, length = 3) private String currency;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemJpaEntity> items = new ArrayList<>();

    protected OrderJpaEntity() {}
    public OrderJpaEntity(UUID id, String customerId, BigDecimal total, String currency, Instant createdAt) {
        this.id = id; this.customerId = customerId; this.total = total; this.currency = currency; this.createdAt = createdAt;
    }
    public void addItem(OrderItemJpaEntity item) { items.add(item); item.setOrder(this); }
    public UUID getId() { return id; }
    public String getCustomerId() { return customerId; }
    public BigDecimal getTotal() { return total; }
    public String getCurrency() { return currency; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderItemJpaEntity> getItems() { return items; }
}

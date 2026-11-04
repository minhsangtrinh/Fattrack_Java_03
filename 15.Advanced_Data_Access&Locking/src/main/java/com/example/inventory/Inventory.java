package com.example.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Check;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "inventories")
@Audited
@Check(constraints = "quantity >= 0")
public class Inventory {
    @Id
    @Column(nullable = false, updatable = false)
    private Long productId;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false)
    private long quantity;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Inventory() {}

    public Inventory(Long productId, String productName, long quantity) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
    }

    public void stockIn(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Stock-in amount must be positive");
        }
        quantity = Math.addExact(quantity, amount);
    }

    public void rename(String productName) {
        this.productName = productName;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public long getQuantity() {
        return quantity;
    }

    public Long getVersion() {
        return version;
    }
}

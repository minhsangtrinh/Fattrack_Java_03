package com.example.orders.domain.order.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount == null || currency == null) throw new IllegalArgumentException("Amount and currency are required");
        if (amount.signum() < 0) throw new IllegalArgumentException("Amount cannot be negative");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }
    public Money plus(Money other) {
        if (!currency.equals(other.currency)) throw new IllegalArgumentException("Currency mismatch");
        return new Money(amount.add(other.amount), currency);
    }
    public Money multiply(int quantity) {
        if (quantity < 1) throw new IllegalArgumentException("Quantity must be positive");
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)), currency);
    }
}

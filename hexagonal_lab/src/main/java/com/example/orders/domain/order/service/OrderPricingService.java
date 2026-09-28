package com.example.orders.domain.order.service;

import com.example.orders.domain.order.entity.Money;
import com.example.orders.domain.order.entity.OrderItem;
import java.util.List;

public class OrderPricingService {
    public Money calculateTotal(List<OrderItem> items) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("An order must contain at least one item");
        Money total = items.getFirst().subtotal();
        for (int i = 1; i < items.size(); i++) total = total.plus(items.get(i).subtotal());
        return total;
    }
}

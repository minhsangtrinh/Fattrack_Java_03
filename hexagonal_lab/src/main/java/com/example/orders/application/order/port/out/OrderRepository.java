package com.example.orders.application.order.port.out;

import com.example.orders.domain.order.entity.Order;

public interface OrderRepository {
    Order save(Order order);
}

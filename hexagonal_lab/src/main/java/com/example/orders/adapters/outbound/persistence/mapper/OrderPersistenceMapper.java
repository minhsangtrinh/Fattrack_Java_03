package com.example.orders.adapters.outbound.persistence.mapper;

import com.example.orders.adapters.outbound.persistence.entity.OrderItemJpaEntity;
import com.example.orders.adapters.outbound.persistence.entity.OrderJpaEntity;
import com.example.orders.domain.order.entity.*;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

public class OrderPersistenceMapper {
    public OrderJpaEntity toEntity(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity(order.id().value(), order.customerId().value(),
                order.total().amount(), order.total().currency().getCurrencyCode(), order.createdAt());
        order.items().forEach(item -> entity.addItem(new OrderItemJpaEntity(UUID.randomUUID(), item.sku(),
                item.quantity(), item.unitPrice().amount(), item.unitPrice().currency().getCurrencyCode())));
        return entity;
    }
    public Order toDomain(OrderJpaEntity entity) {
        List<OrderItem> items = entity.getItems().stream().map(i -> new OrderItem(i.getSku(), i.getQuantity(),
                new Money(i.getUnitPrice(), Currency.getInstance(i.getCurrency())))).toList();
        return new Order(new OrderId(entity.getId()), new CustomerId(entity.getCustomerId()), items,
                new Money(entity.getTotal(), Currency.getInstance(entity.getCurrency())), entity.getCreatedAt());
    }
}

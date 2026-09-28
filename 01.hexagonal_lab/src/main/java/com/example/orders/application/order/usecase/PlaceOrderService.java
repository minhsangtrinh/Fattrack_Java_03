package com.example.orders.application.order.usecase;

import com.example.orders.application.order.dto.PlaceOrderCommand;
import com.example.orders.application.order.dto.PlaceOrderResult;
import com.example.orders.application.order.port.in.PlaceOrderUseCase;
import com.example.orders.application.order.port.out.OrderRepository;
import com.example.orders.domain.order.entity.*;
import com.example.orders.domain.order.service.OrderPricingService;
import java.util.Currency;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public class PlaceOrderService implements PlaceOrderUseCase {
    private final OrderRepository repository;
    private final OrderPricingService pricingService;

    public PlaceOrderService(OrderRepository repository, OrderPricingService pricingService) {
        this.repository = repository;
        this.pricingService = pricingService;
    }

    @Override
    @Transactional
    public PlaceOrderResult placeOrder(PlaceOrderCommand command) {
        if (command == null || command.items() == null || command.items().isEmpty())
            throw new IllegalArgumentException("At least one order item is required");
        Currency currency = Currency.getInstance(command.currency());
        List<OrderItem> items = command.items().stream()
                .map(item -> new OrderItem(item.sku(), item.quantity(), new Money(item.unitPrice(), currency)))
                .toList();
        Order order = Order.place(OrderId.generate(), new CustomerId(command.customerId()), items,
                pricingService.calculateTotal(items));
        Order saved = repository.save(order);
        return new PlaceOrderResult(saved.id().value(), saved.total().amount(),
                saved.total().currency().getCurrencyCode(), saved.createdAt());
    }
}

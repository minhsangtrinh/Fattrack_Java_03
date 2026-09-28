package com.example.orders.application.order.port.in;

import com.example.orders.application.order.dto.PlaceOrderCommand;
import com.example.orders.application.order.dto.PlaceOrderResult;

public interface PlaceOrderUseCase {
    PlaceOrderResult placeOrder(PlaceOrderCommand command);
}

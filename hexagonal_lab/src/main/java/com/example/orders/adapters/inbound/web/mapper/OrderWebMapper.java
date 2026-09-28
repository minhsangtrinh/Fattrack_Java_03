package com.example.orders.adapters.inbound.web.mapper;

import com.example.orders.adapters.inbound.web.request.PlaceOrderRequest;
import com.example.orders.adapters.inbound.web.response.PlaceOrderResponse;
import com.example.orders.application.order.dto.PlaceOrderCommand;
import com.example.orders.application.order.dto.PlaceOrderResult;

public class OrderWebMapper {
    public PlaceOrderCommand toCommand(PlaceOrderRequest request) {
        return new PlaceOrderCommand(request.customerId(), request.currency(),
                request.items().stream().map(i -> new PlaceOrderCommand.Item(i.sku(), i.quantity(), i.unitPrice())).toList());
    }
    public PlaceOrderResponse toResponse(PlaceOrderResult result) {
        return new PlaceOrderResponse(result.orderId(), result.total(), result.currency(), result.createdAt());
    }
}

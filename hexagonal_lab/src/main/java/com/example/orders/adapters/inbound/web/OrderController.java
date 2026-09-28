package com.example.orders.adapters.inbound.web;

import com.example.orders.adapters.inbound.web.mapper.OrderWebMapper;
import com.example.orders.adapters.inbound.web.request.PlaceOrderRequest;
import com.example.orders.adapters.inbound.web.response.PlaceOrderResponse;
import com.example.orders.application.order.port.in.PlaceOrderUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final PlaceOrderUseCase placeOrder;
    private final OrderWebMapper mapper;

    public OrderController(PlaceOrderUseCase placeOrder, OrderWebMapper mapper) {
        this.placeOrder = placeOrder;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlaceOrderResponse placeOrder(@RequestBody PlaceOrderRequest request) {
        return mapper.toResponse(placeOrder.placeOrder(mapper.toCommand(request)));
    }
}

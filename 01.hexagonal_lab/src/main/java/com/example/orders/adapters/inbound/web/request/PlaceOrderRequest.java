package com.example.orders.adapters.inbound.web.request;

import java.math.BigDecimal;
import java.util.List;

public record PlaceOrderRequest(String customerId, String currency, List<PlaceOrderItemRequest> items) {}

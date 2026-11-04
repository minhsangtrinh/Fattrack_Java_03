package com.example.orders.adapters.inbound.web.request;

import java.math.BigDecimal;

public record PlaceOrderItemRequest(String sku, int quantity, BigDecimal unitPrice) {}

package com.example.orders.application.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlaceOrderResult(UUID orderId, BigDecimal total, String currency, Instant createdAt) {}

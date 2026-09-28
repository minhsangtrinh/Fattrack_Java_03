package com.example.orders.adapters.inbound.web.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlaceOrderResponse(UUID orderId, BigDecimal total, String currency, Instant createdAt) {}

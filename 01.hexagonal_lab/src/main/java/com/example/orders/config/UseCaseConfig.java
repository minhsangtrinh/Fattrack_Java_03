package com.example.orders.config;

import com.example.orders.adapters.inbound.web.mapper.OrderWebMapper;
import com.example.orders.adapters.outbound.persistence.mapper.OrderPersistenceMapper;
import com.example.orders.application.order.port.in.PlaceOrderUseCase;
import com.example.orders.application.order.port.out.OrderRepository;
import com.example.orders.application.order.usecase.PlaceOrderService;
import com.example.orders.domain.order.service.OrderPricingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {
    @Bean OrderPricingService orderPricingService() { return new OrderPricingService(); }
    @Bean PlaceOrderUseCase placeOrderUseCase(OrderRepository repository, OrderPricingService pricing) {
        return new PlaceOrderService(repository, pricing);
    }
    @Bean OrderWebMapper orderWebMapper() { return new OrderWebMapper(); }
    @Bean OrderPersistenceMapper orderPersistenceMapper() { return new OrderPersistenceMapper(); }
}

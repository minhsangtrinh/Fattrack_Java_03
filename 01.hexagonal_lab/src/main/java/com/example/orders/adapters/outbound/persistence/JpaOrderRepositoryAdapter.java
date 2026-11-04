package com.example.orders.adapters.outbound.persistence;

import com.example.orders.adapters.outbound.persistence.mapper.OrderPersistenceMapper;
import com.example.orders.adapters.outbound.persistence.springdata.SpringDataOrderRepository;
import com.example.orders.application.order.port.out.OrderRepository;
import com.example.orders.domain.order.entity.Order;
import org.springframework.stereotype.Repository;

@Repository
public class JpaOrderRepositoryAdapter implements OrderRepository {
    private final SpringDataOrderRepository repository;
    private final OrderPersistenceMapper mapper;
    public JpaOrderRepositoryAdapter(SpringDataOrderRepository repository, OrderPersistenceMapper mapper) {
        this.repository = repository; this.mapper = mapper;
    }
    @Override public Order save(Order order) { return mapper.toDomain(repository.save(mapper.toEntity(order))); }
}

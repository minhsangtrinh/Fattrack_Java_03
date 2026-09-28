package com.example.orders.adapters.outbound.persistence.springdata;

import com.example.orders.adapters.outbound.persistence.entity.OrderJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {}

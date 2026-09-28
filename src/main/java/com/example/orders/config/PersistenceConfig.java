package com.example.orders.config;

import com.example.orders.adapters.outbound.persistence.entity.OrderJpaEntity;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackageClasses = OrderJpaEntity.class)
@EnableJpaRepositories(basePackages = "com.example.orders.adapters.outbound.persistence.springdata")
public class PersistenceConfig {}

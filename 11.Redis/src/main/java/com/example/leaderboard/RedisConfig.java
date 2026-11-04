package com.example.leaderboard;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {
    @Bean
    @Primary
    public LettuceConnectionFactory primaryRedisConnectionFactory(
            @Value("${leaderboard.redis.primary.host}") String host,
            @Value("${leaderboard.redis.primary.port}") int port) {
        return connectionFactory(host, port);
    }

    @Bean
    public LettuceConnectionFactory replicaRedisConnectionFactory(
            @Value("${leaderboard.redis.replica.host}") String host,
            @Value("${leaderboard.redis.replica.port}") int port) {
        return connectionFactory(host, port);
    }

    @Bean
    public StringRedisTemplate primaryRedisTemplate(
            @Qualifier("primaryRedisConnectionFactory") LettuceConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    @Bean
    public StringRedisTemplate replicaRedisTemplate(
            @Qualifier("replicaRedisConnectionFactory") LettuceConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    private LettuceConnectionFactory connectionFactory(String host, int port) {
        var server = new RedisStandaloneConfiguration(host, port);
        var client = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(2))
                .shutdownTimeout(Duration.ofMillis(100))
                .build();
        return new LettuceConnectionFactory(server, client);
    }
}

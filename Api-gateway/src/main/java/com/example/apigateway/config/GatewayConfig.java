package com.example.apigateway.config;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import java.time.Duration;
/**
 * Gateway infrastructure configuration:
 *  - Provides an IP-based KeyResolver for RequestRateLimiter (required bean).
 *  - Configures a 5-second CircuitBreaker TimeLimiter so the CB does not fire
 *    prematurely on slightly-slow downstream services during cold start.
 */
@Configuration
public class GatewayConfig {
    @Bean
    public KeyResolver remoteAddrKeyResolver() {
        return exchange -> Mono.justOrEmpty(
                exchange.getRequest().getRemoteAddress() != null
                        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                        : "unknown"
        );
    }
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCircuitBreakerCustomizer() {
        return factory -> factory.configureDefault(id ->
                new Resilience4JConfigBuilder(id)
                        .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
                        .timeLimiterConfig(
                                TimeLimiterConfig.custom()
                                        .timeoutDuration(Duration.ofSeconds(5))
                                        .build())
                        .build()
        );
    }
}
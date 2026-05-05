package com.example.accountservices.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    /** Propagate Correlation-ID and tracing headers on every outbound Feign call */
    @Bean
    public RequestInterceptor correlationIdInterceptor() {
        return template -> {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                String cid = sra.getRequest().getHeader("X-Correlation-ID");
                if (cid != null) template.header("X-Correlation-ID", cid);
            }
        };
    }
}


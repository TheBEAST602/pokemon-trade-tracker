package com.pokemontracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** API key filter for v1 (single user). Replace with real login if the data becomes sensitive. */
@Configuration
public class SecurityConfig {

    @Bean
    FilterRegistrationBean<ApiKeyFilter> apiKeyFilter(@Value("${app.api-key}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("APP_API_KEY must not be empty");
        }
        FilterRegistrationBean<ApiKeyFilter> registration = new FilterRegistrationBean<>(new ApiKeyFilter(apiKey));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}

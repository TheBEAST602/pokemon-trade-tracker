package com.pokemontracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * CORS runs as a servlet filter ahead of the API key filter, so even 401 responses
 * carry CORS headers and the browser shows the real error instead of a network failure.
 */
@Configuration
public class CorsConfig {

    @Bean
    FilterRegistrationBean<CorsFilter> corsFilter(@Value("${app.allowed-origin}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        for (String origin : allowedOrigins.split(",")) {
            String cleaned = origin.trim().replaceAll("/+$", "");
            if (!cleaned.isEmpty()) {
                config.addAllowedOrigin(cleaned);
            }
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-API-Key"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
        return registration;
    }
}

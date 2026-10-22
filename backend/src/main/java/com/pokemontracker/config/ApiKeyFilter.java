package com.pokemontracker.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Requires the X-API-Key header on every /api request except CORS preflight and the health check. */
public class ApiKeyFilter extends OncePerRequestFilter {

    static final String HEADER = "X-API-Key";
    private static final String HEALTH_PATH = "/api/v1/health";

    private final byte[] expectedKey;

    public ApiKeyFilter(String apiKey) {
        this.expectedKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod()) || HEALTH_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        boolean valid = provided != null
                && MessageDigest.isEqual(expectedKey, provided.getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Missing or invalid API key\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}

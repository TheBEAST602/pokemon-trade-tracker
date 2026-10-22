package com.pokemontracker.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Public (no API key) so Render's health check and uptime pings can reach it. */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}

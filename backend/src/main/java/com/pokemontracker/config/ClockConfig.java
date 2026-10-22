package com.pokemontracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** "Today" and the daily special-trade reset follow this zone (default Asia/Kolkata). */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock(@Value("${app.timezone:Asia/Kolkata}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}

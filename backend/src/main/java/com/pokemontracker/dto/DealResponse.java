package com.pokemontracker.dto;

import com.pokemontracker.model.DealStatus;
import com.pokemontracker.model.Priority;
import com.pokemontracker.model.TradeType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record DealResponse(
        UUID id,
        String friendName,
        String trainerCode,
        String pokemonGive,
        String pokemonGet,
        TradeType tradeType,
        int planDays,
        LocalDate dealDate,
        LocalDate dueDate,
        Priority priority,
        DealStatus status,
        boolean specialTrade,
        String platform,
        String chatLink,
        String notes,
        LocalDate completedOn,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long daysLeft,
        boolean overdue
) {
}

package com.pokemontracker.mapper;

import com.pokemontracker.dto.DealResponse;
import com.pokemontracker.model.Deal;
import com.pokemontracker.model.DealStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class DealMapper {

    /** daysLeft and overdue are derived from {@code today}, never stored. */
    public DealResponse toResponse(Deal deal, LocalDate today) {
        long daysLeft = ChronoUnit.DAYS.between(today, deal.getDueDate());
        boolean overdue = deal.getStatus() == DealStatus.PLANNED && daysLeft < 0;
        return new DealResponse(
                deal.getId(),
                deal.getFriendName(),
                deal.getTrainerCode(),
                deal.getPokemonGive(),
                deal.getPokemonGet(),
                deal.getTradeType(),
                deal.getPlanDays(),
                deal.getDealDate(),
                deal.getDueDate(),
                deal.getPriority(),
                deal.getStatus(),
                deal.isSpecialTrade(),
                deal.getPlatform(),
                deal.getChatLink(),
                deal.getNotes(),
                deal.getCompletedOn(),
                deal.getCreatedAt(),
                deal.getUpdatedAt(),
                daysLeft,
                overdue);
    }
}

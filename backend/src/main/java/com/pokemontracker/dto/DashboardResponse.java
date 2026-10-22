package com.pokemontracker.dto;

import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        LocalDate today,
        List<DealResponse> overdue,
        List<DealResponse> dueToday,
        List<DealResponse> upcoming,
        boolean specialTradeUsedToday,
        long plannedCount
) {
}

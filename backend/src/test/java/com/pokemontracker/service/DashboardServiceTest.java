package com.pokemontracker.service;

import com.pokemontracker.dto.DashboardResponse;
import com.pokemontracker.mapper.DealMapper;
import com.pokemontracker.model.Deal;
import com.pokemontracker.model.DealStatus;
import com.pokemontracker.model.Priority;
import com.pokemontracker.model.TradeType;
import com.pokemontracker.repository.DealRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 19);

    @Mock
    private DealRepository repository;

    private DashboardService serviceAt(String utcInstant) {
        Clock clock = Clock.fixed(Instant.parse(utcInstant), IST);
        return new DashboardService(repository, new DealMapper(), clock);
    }

    @Test
    void groupsDealsIntoOverdueTodayAndUpcomingBuckets() {
        Deal late = deal("late", Priority.HIGH, TODAY.minusDays(2));
        Deal now = deal("now", Priority.MEDIUM, TODAY);
        Deal soon = deal("soon", Priority.LOW, TODAY.plusDays(3));
        when(repository.findByStatusAndDueDateBefore(DealStatus.PLANNED, TODAY)).thenReturn(List.of(late));
        when(repository.findByStatusAndDueDate(DealStatus.PLANNED, TODAY)).thenReturn(List.of(now));
        when(repository.findByStatusAndDueDateBetween(DealStatus.PLANNED, TODAY.plusDays(1), TODAY.plusDays(7)))
                .thenReturn(List.of(soon));
        when(repository.countByStatus(DealStatus.PLANNED)).thenReturn(3L);

        // 2026-10-19 11:30 IST
        DashboardResponse response = serviceAt("2026-10-19T06:00:00Z").today();

        assertEquals(TODAY, response.today());
        assertEquals(1, response.overdue().size());
        assertEquals(-2, response.overdue().get(0).daysLeft());
        assertTrue(response.overdue().get(0).overdue());
        assertEquals(0, response.dueToday().get(0).daysLeft());
        assertFalse(response.dueToday().get(0).overdue());
        assertEquals(3, response.upcoming().get(0).daysLeft());
        assertEquals(3L, response.plannedCount());
    }

    @Test
    void bucketsAreSortedByPriorityThenDueDate() {
        Deal lowSoon = deal("low", Priority.LOW, TODAY.plusDays(1));
        Deal highLater = deal("high", Priority.HIGH, TODAY.plusDays(6));
        when(repository.findByStatusAndDueDateBetween(DealStatus.PLANNED, TODAY.plusDays(1), TODAY.plusDays(7)))
                .thenReturn(List.of(lowSoon, highLater));

        DashboardResponse response = serviceAt("2026-10-19T06:00:00Z").today();

        assertEquals("high", response.upcoming().get(0).friendName());
        assertEquals("low", response.upcoming().get(1).friendName());
    }

    @Test
    void reportsWhetherTheSpecialTradeWasUsedToday() {
        when(repository.existsByStatusAndSpecialTradeTrueAndCompletedOn(DealStatus.DONE, TODAY)).thenReturn(true);

        assertTrue(serviceAt("2026-10-19T06:00:00Z").today().specialTradeUsedToday());
    }

    @Test
    void daysLeftRollsOverAtLocalMidnight() {
        Deal dueOct20 = deal("tomorrow", Priority.MEDIUM, LocalDate.of(2026, 10, 20));
        // 23:59 IST on 2026-10-19 -> due tomorrow, shows in "upcoming" with 1 day left
        lenient().when(repository.findByStatusAndDueDateBetween(
                DealStatus.PLANNED, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 26)))
                .thenReturn(List.of(dueOct20));
        // 00:01 IST on 2026-10-20 -> the same deal is now due today with 0 days left
        lenient().when(repository.findByStatusAndDueDate(DealStatus.PLANNED, LocalDate.of(2026, 10, 20)))
                .thenReturn(List.of(dueOct20));

        DashboardResponse beforeMidnight = serviceAt("2026-10-19T18:29:00Z").today();
        DashboardResponse afterMidnight = serviceAt("2026-10-19T18:31:00Z").today();

        assertEquals(LocalDate.of(2026, 10, 19), beforeMidnight.today());
        assertEquals(1, beforeMidnight.upcoming().get(0).daysLeft());
        assertTrue(beforeMidnight.dueToday().isEmpty());

        assertEquals(LocalDate.of(2026, 10, 20), afterMidnight.today());
        assertEquals(0, afterMidnight.dueToday().get(0).daysLeft());
    }

    private static Deal deal(String friend, Priority priority, LocalDate due) {
        Deal deal = new Deal();
        deal.setId(UUID.randomUUID());
        deal.setFriendName(friend);
        deal.setPokemonGive("Pikachu");
        deal.setPokemonGet("Eevee");
        deal.setTradeType(TradeType.CUSTOM);
        deal.setDealDate(TODAY.minusDays(10));
        deal.setDueDate(due);
        deal.setPriority(priority);
        deal.setStatus(DealStatus.PLANNED);
        return deal;
    }
}

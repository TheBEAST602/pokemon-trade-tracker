package com.pokemontracker.service;

import com.pokemontracker.dto.DashboardResponse;
import com.pokemontracker.dto.DealResponse;
import com.pokemontracker.mapper.DealMapper;
import com.pokemontracker.model.Deal;
import com.pokemontracker.model.DealStatus;
import com.pokemontracker.repository.DealRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class DashboardService {

    static final int UPCOMING_WINDOW_DAYS = 7;

    private static final Comparator<DealResponse> ORDER =
            Comparator.comparing(DealResponse::priority).thenComparing(DealResponse::dueDate);

    private final DealRepository repository;
    private final DealMapper mapper;
    private final Clock clock;

    public DashboardService(DealRepository repository, DealMapper mapper, Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse today() {
        LocalDate today = LocalDate.now(clock);

        List<DealResponse> overdue = toSortedResponses(
                repository.findByStatusAndDueDateBefore(DealStatus.PLANNED, today), today);
        List<DealResponse> dueToday = toSortedResponses(
                repository.findByStatusAndDueDate(DealStatus.PLANNED, today), today);
        List<DealResponse> upcoming = toSortedResponses(
                repository.findByStatusAndDueDateBetween(
                        DealStatus.PLANNED, today.plusDays(1), today.plusDays(UPCOMING_WINDOW_DAYS)), today);

        boolean specialUsed = repository.existsByStatusAndSpecialTradeTrueAndCompletedOn(DealStatus.DONE, today);
        long plannedCount = repository.countByStatus(DealStatus.PLANNED);

        return new DashboardResponse(today, overdue, dueToday, upcoming, specialUsed, plannedCount);
    }

    private List<DealResponse> toSortedResponses(List<Deal> deals, LocalDate today) {
        return deals.stream()
                .map(d -> mapper.toResponse(d, today))
                .sorted(ORDER)
                .toList();
    }
}

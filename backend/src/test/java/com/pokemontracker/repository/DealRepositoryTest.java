package com.pokemontracker.repository;

import com.pokemontracker.model.Deal;
import com.pokemontracker.model.DealStatus;
import com.pokemontracker.model.Priority;
import com.pokemontracker.model.TradeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs against in-memory H2; Hibernate builds the schema from the entity, so Flyway is switched off here. */
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class DealRepositoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 19);

    @Autowired
    private DealRepository repository;

    private Deal overdue;
    private Deal dueToday;
    private Deal upcoming;
    private Deal farFuture;
    private Deal doneSpecial;

    @BeforeEach
    void seed() {
        overdue = repository.save(deal("overdue", TODAY.minusDays(2), DealStatus.PLANNED, false, null));
        dueToday = repository.save(deal("today", TODAY, DealStatus.PLANNED, false, null));
        upcoming = repository.save(deal("upcoming", TODAY.plusDays(5), DealStatus.PLANNED, false, null));
        farFuture = repository.save(deal("far", TODAY.plusDays(30), DealStatus.PLANNED, false, null));
        doneSpecial = repository.save(deal("done", TODAY.minusDays(1), DealStatus.DONE, true, TODAY));
    }

    @Test
    void savePopulatesIdAndAuditTimestamps() {
        assertNotNull(overdue.getId());
        assertNotNull(overdue.getCreatedAt());
        assertNotNull(overdue.getUpdatedAt());
    }

    @Test
    void findsOnlyPlannedDealsDueBeforeADate() {
        List<Deal> result = repository.findByStatusAndDueDateBefore(DealStatus.PLANNED, TODAY);

        assertEquals(List.of(overdue.getId()), result.stream().map(Deal::getId).toList());
    }

    @Test
    void findsPlannedDealsDueOnADate() {
        List<Deal> result = repository.findByStatusAndDueDate(DealStatus.PLANNED, TODAY);

        assertEquals(List.of(dueToday.getId()), result.stream().map(Deal::getId).toList());
    }

    @Test
    void betweenQueryIsInclusiveAndExcludesOtherWindows() {
        List<Deal> result = repository.findByStatusAndDueDateBetween(
                DealStatus.PLANNED, TODAY.plusDays(1), TODAY.plusDays(7));

        assertEquals(List.of(upcoming.getId()), result.stream().map(Deal::getId).toList());
    }

    @Test
    void countsDealsByStatus() {
        assertEquals(4, repository.countByStatus(DealStatus.PLANNED));
        assertEquals(1, repository.countByStatus(DealStatus.DONE));
        assertEquals(0, repository.countByStatus(DealStatus.CANCELLED));
    }

    @Test
    void detectsSpecialTradeAlreadyCompletedOnADay() {
        assertTrue(repository.existsByStatusAndSpecialTradeTrueAndCompletedOn(DealStatus.DONE, TODAY));
        assertFalse(repository.existsByStatusAndSpecialTradeTrueAndCompletedOn(
                DealStatus.DONE, TODAY.minusDays(1)));
    }

    @Test
    void specialTradeCheckCanExcludeTheDealItself() {
        assertFalse(repository.existsByStatusAndSpecialTradeTrueAndCompletedOnAndIdNot(
                DealStatus.DONE, TODAY, doneSpecial.getId()));
        assertTrue(repository.existsByStatusAndSpecialTradeTrueAndCompletedOnAndIdNot(
                DealStatus.DONE, TODAY, overdue.getId()));
    }

    private static Deal deal(String friend, LocalDate due, DealStatus status, boolean special, LocalDate completedOn) {
        Deal deal = new Deal();
        deal.setFriendName(friend);
        deal.setPokemonGive("Pikachu");
        deal.setPokemonGet("Eevee");
        deal.setTradeType(TradeType.CUSTOM);
        deal.setPlanDays(0);
        deal.setDealDate(TODAY.minusDays(10));
        deal.setDueDate(due);
        deal.setPriority(Priority.MEDIUM);
        deal.setStatus(status);
        deal.setSpecialTrade(special);
        deal.setCompletedOn(completedOn);
        return deal;
    }
}

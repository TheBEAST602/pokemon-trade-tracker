package com.pokemontracker.service;

import com.pokemontracker.dto.DealRequest;
import com.pokemontracker.dto.DealResponse;
import com.pokemontracker.exception.DealNotFoundException;
import com.pokemontracker.exception.InvalidDealException;
import com.pokemontracker.exception.SpecialTradeAlreadyUsedException;
import com.pokemontracker.mapper.DealMapper;
import com.pokemontracker.model.Deal;
import com.pokemontracker.model.DealStatus;
import com.pokemontracker.model.Priority;
import com.pokemontracker.model.TradeType;
import com.pokemontracker.repository.DealRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DealServiceTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    /** 2026-10-19 11:30 in Asia/Kolkata. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-19T06:00:00Z"), IST);
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 19);

    @Mock
    private DealRepository repository;

    private DealService service;

    @BeforeEach
    void setUp() {
        service = new DealService(repository, new DealMapper(), CLOCK);
        lenient().when(repository.save(any(Deal.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---- dueDate calculation ---------------------------------------------

    @ParameterizedTest
    @CsvSource({"IMMEDIATE,0", "DAYS_7,7", "DAYS_30,30"})
    void dueDateIsDealDatePlusDefaultDaysForEachTradeType(TradeType type, int expectedDays) {
        DealResponse response = service.create(request(type, null, "https://wa.me/12345"));

        assertEquals(expectedDays, response.planDays());
        assertEquals(TODAY.plusDays(expectedDays), response.dueDate());
        assertEquals(DealStatus.PLANNED, response.status());
    }

    @Test
    void customTradeTypeUsesSuppliedPlanDays() {
        DealResponse response = service.create(request(TradeType.CUSTOM, 12, null));

        assertEquals(12, response.planDays());
        assertEquals(LocalDate.of(2026, 10, 31), response.dueDate());
    }

    @Test
    void customTradeTypeWithoutPlanDaysIsRejected() {
        assertThrows(InvalidDealException.class, () -> service.create(request(TradeType.CUSTOM, null, null)));
    }

    @Test
    void presetTradeTypeIgnoresSuppliedPlanDays() {
        DealResponse response = service.create(request(TradeType.DAYS_7, 99, null));

        assertEquals(7, response.planDays());
    }

    @Test
    void dealDateDefaultsToToday() {
        DealResponse response = service.create(new DealRequest("Ash", null, "Pikachu", "Eevee",
                TradeType.DAYS_7, null, null, null, null, null, null, null));

        assertEquals(TODAY, response.dealDate());
        assertEquals(TODAY.plusDays(7), response.dueDate());
        assertEquals(Priority.MEDIUM, response.priority());
    }

    @Test
    void updateRecomputesDueDate() {
        Deal existing = deal(UUID.randomUUID(), DealStatus.PLANNED, false);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        DealResponse response = service.update(existing.getId(), request(TradeType.DAYS_30, null, null));

        assertEquals(TODAY.plusDays(30), response.dueDate());
        assertEquals(30, response.planDays());
    }

    // ---- chat link rules -------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"http://wa.me/12345", "wa.me/12345", "ftp://example.com/x",
            "javascript:alert(1)", "https://", "https://exa mple.com"})
    void invalidChatLinkIsRejected(String link) {
        assertThrows(InvalidDealException.class,
                () -> service.create(request(TradeType.IMMEDIATE, null, link)));
        verify(repository, never()).save(any(Deal.class));
    }

    @Test
    void chatLinkLongerThan500CharactersIsRejected() {
        String link = "https://example.com/" + "a".repeat(500);

        assertThrows(InvalidDealException.class,
                () -> service.create(request(TradeType.IMMEDIATE, null, link)));
    }

    @Test
    void errorMessageForNonHttpsLinkIsClear() {
        InvalidDealException ex = assertThrows(InvalidDealException.class,
                () -> service.create(request(TradeType.IMMEDIATE, null, "http://wa.me/1")));

        assertEquals("chatLink must start with https://", ex.getMessage());
    }

    @Test
    void blankChatLinkIsStoredAsNull() {
        DealResponse response = service.create(request(TradeType.IMMEDIATE, null, "  "));

        assertNull(response.chatLink());
    }

    // ---- status changes --------------------------------------------------

    @Test
    void markingDoneSetsCompletedOnToToday() {
        Deal existing = deal(UUID.randomUUID(), DealStatus.PLANNED, false);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        DealResponse response = service.updateStatus(existing.getId(), DealStatus.DONE);

        assertEquals(DealStatus.DONE, response.status());
        assertEquals(TODAY, response.completedOn());
    }

    @Test
    void reopeningClearsCompletedOn() {
        Deal existing = deal(UUID.randomUUID(), DealStatus.DONE, false);
        existing.setCompletedOn(TODAY);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        DealResponse response = service.updateStatus(existing.getId(), DealStatus.PLANNED);

        assertEquals(DealStatus.PLANNED, response.status());
        assertNull(response.completedOn());
    }

    @Test
    void secondSpecialTradeOnTheSameDayIsRejected() {
        Deal existing = deal(UUID.randomUUID(), DealStatus.PLANNED, true);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(repository.existsByStatusAndSpecialTradeTrueAndCompletedOnAndIdNot(
                DealStatus.DONE, TODAY, existing.getId())).thenReturn(true);

        SpecialTradeAlreadyUsedException ex = assertThrows(SpecialTradeAlreadyUsedException.class,
                () -> service.updateStatus(existing.getId(), DealStatus.DONE));

        assertTrue(ex.getMessage().contains("2026-10-19"));
        assertEquals(DealStatus.PLANNED, existing.getStatus());
        assertNull(existing.getCompletedOn());
    }

    @Test
    void firstSpecialTradeOfTheDayIsAllowed() {
        Deal existing = deal(UUID.randomUUID(), DealStatus.PLANNED, true);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(repository.existsByStatusAndSpecialTradeTrueAndCompletedOnAndIdNot(
                DealStatus.DONE, TODAY, existing.getId())).thenReturn(false);

        DealResponse response = service.updateStatus(existing.getId(), DealStatus.DONE);

        assertEquals(DealStatus.DONE, response.status());
        assertTrue(response.specialTrade());
    }

    @Test
    void nonSpecialDealNeverChecksTheSpecialTradeLimit() {
        Deal existing = deal(UUID.randomUUID(), DealStatus.PLANNED, false);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        service.updateStatus(existing.getId(), DealStatus.DONE);

        verify(repository, never()).existsByStatusAndSpecialTradeTrueAndCompletedOnAndIdNot(
                any(), any(), any());
    }

    @Test
    void cancellingDoesNotSetCompletedOn() {
        Deal existing = deal(UUID.randomUUID(), DealStatus.PLANNED, true);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        DealResponse response = service.updateStatus(existing.getId(), DealStatus.CANCELLED);

        assertEquals(DealStatus.CANCELLED, response.status());
        assertNull(response.completedOn());
        assertFalse(response.overdue());
    }

    // ---- misc ------------------------------------------------------------

    @Test
    void updatePriorityChangesOnlyPriority() {
        Deal existing = deal(UUID.randomUUID(), DealStatus.PLANNED, false);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        DealResponse response = service.updatePriority(existing.getId(), Priority.HIGH);

        assertEquals(Priority.HIGH, response.priority());
        assertEquals("Ash", response.friendName());
    }

    @Test
    void unknownIdThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(DealNotFoundException.class, () -> service.get(id));
    }

    @Test
    void unknownSortValueIsRejected() {
        assertThrows(InvalidDealException.class, () -> service.list(null, null, null, null, null, "friend"));
    }

    @Test
    void defaultListOrderIsOverdueThenHighPriorityThenNearestDueDate() {
        Deal lowOverdue = dealDue("low-overdue", Priority.LOW, TODAY.minusDays(3));
        Deal highSoon = dealDue("high-soon", Priority.HIGH, TODAY.plusDays(1));
        Deal mediumSoon = dealDue("medium-soon", Priority.MEDIUM, TODAY.plusDays(1));
        Deal highLater = dealDue("high-later", Priority.HIGH, TODAY.plusDays(9));
        when(repository.findAll(ArgumentMatchers.<Specification<Deal>>any()))
                .thenReturn(List.of(highLater, mediumSoon, highSoon, lowOverdue));

        List<String> order = service.list(null, null, null, null, null, null).stream()
                .map(DealResponse::friendName).toList();

        assertEquals(List.of("low-overdue", "high-soon", "high-later", "medium-soon"), order);
    }

    @Test
    void sortByDueDateOrdersNearestFirst() {
        Deal later = dealDue("later", Priority.HIGH, TODAY.plusDays(9));
        Deal sooner = dealDue("sooner", Priority.LOW, TODAY.plusDays(2));
        when(repository.findAll(ArgumentMatchers.<Specification<Deal>>any())).thenReturn(List.of(later, sooner));

        List<String> order = service.list(null, null, null, null, null, "dueDate").stream()
                .map(DealResponse::friendName).toList();

        assertEquals(List.of("sooner", "later"), order);
    }

    @Test
    void overdueFilterKeepsOnlyOverduePlannedDeals() {
        Deal late = dealDue("late", Priority.MEDIUM, TODAY.minusDays(1));
        Deal fine = dealDue("fine", Priority.MEDIUM, TODAY.plusDays(1));
        when(repository.findAll(ArgumentMatchers.<Specification<Deal>>any())).thenReturn(List.of(late, fine));

        List<DealResponse> result = service.list(null, null, null, null, true, null);

        assertEquals(1, result.size());
        assertEquals("late", result.get(0).friendName());
        assertEquals(-1, result.get(0).daysLeft());
    }

    // ---- fixtures --------------------------------------------------------

    private static DealRequest request(TradeType type, Integer planDays, String chatLink) {
        return new DealRequest("Ash", "1234 5678 9012", "Pikachu", "Eevee", type, planDays,
                TODAY, Priority.HIGH, false, "WhatsApp", chatLink, "note");
    }

    private static Deal deal(UUID id, DealStatus status, boolean special) {
        Deal deal = dealDue("Ash", Priority.MEDIUM, TODAY.plusDays(7));
        deal.setId(id);
        deal.setStatus(status);
        deal.setSpecialTrade(special);
        return deal;
    }

    private static Deal dealDue(String friend, Priority priority, LocalDate due) {
        Deal deal = new Deal();
        deal.setId(UUID.randomUUID());
        deal.setFriendName(friend);
        deal.setPokemonGive("Pikachu");
        deal.setPokemonGet("Eevee");
        deal.setTradeType(TradeType.CUSTOM);
        deal.setDealDate(TODAY);
        deal.setDueDate(due);
        deal.setPlanDays((int) java.time.temporal.ChronoUnit.DAYS.between(TODAY, due));
        deal.setPriority(priority);
        deal.setStatus(DealStatus.PLANNED);
        return deal;
    }
}

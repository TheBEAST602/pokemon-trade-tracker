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
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DealService {

    static final int MAX_LINK_LENGTH = 500;

    private static final Comparator<DealResponse> BY_PRIORITY_THEN_DUE =
            Comparator.comparing(DealResponse::priority).thenComparing(DealResponse::dueDate);

    private static final Comparator<DealResponse> BY_DUE_THEN_PRIORITY =
            Comparator.comparing(DealResponse::dueDate).thenComparing(DealResponse::priority);

    /** Rule 6: overdue first, then HIGH priority, then nearest due date. */
    private static final Comparator<DealResponse> DEFAULT_ORDER =
            Comparator.comparing((DealResponse r) -> !r.overdue()).thenComparing(BY_PRIORITY_THEN_DUE);

    private final DealRepository repository;
    private final DealMapper mapper;
    private final Clock clock;

    public DealService(DealRepository repository, DealMapper mapper, Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public DealResponse create(DealRequest request) {
        Deal deal = new Deal();
        applyRequest(deal, request);
        deal.setStatus(DealStatus.PLANNED);
        return toResponse(repository.save(deal));
    }

    @Transactional(readOnly = true)
    public DealResponse get(UUID id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public List<DealResponse> list(DealStatus status, Priority priority, TradeType tradeType,
                                   String platform, Boolean overdue, String sort) {
        Comparator<DealResponse> order = resolveOrder(sort);

        Specification<Deal> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (tradeType != null) {
                predicates.add(cb.equal(root.get("tradeType"), tradeType));
            }
            if (platform != null && !platform.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("platform")), platform.trim().toLowerCase(Locale.ROOT)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return repository.findAll(spec).stream()
                .map(this::toResponse)
                .filter(r -> overdue == null || r.overdue() == overdue)
                .sorted(order)
                .toList();
    }

    @Transactional
    public DealResponse update(UUID id, DealRequest request) {
        Deal deal = find(id);
        applyRequest(deal, request);
        if (deal.getStatus() == DealStatus.DONE && deal.isSpecialTrade()) {
            assertSpecialTradeAvailable(deal, deal.getCompletedOn());
        }
        return toResponse(deal);
    }

    @Transactional
    public DealResponse updatePriority(UUID id, Priority priority) {
        Deal deal = find(id);
        deal.setPriority(priority);
        return toResponse(deal);
    }

    @Transactional
    public DealResponse updateStatus(UUID id, DealStatus target) {
        Deal deal = find(id);
        if (target == DealStatus.DONE) {
            if (deal.getStatus() != DealStatus.DONE) {
                LocalDate today = today();
                if (deal.isSpecialTrade()) {
                    assertSpecialTradeAvailable(deal, today);
                }
                deal.setCompletedOn(today);
            }
        } else {
            deal.setCompletedOn(null);
        }
        deal.setStatus(target);
        return toResponse(deal);
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(find(id));
    }

    // ---- helpers -------------------------------------------------------

    private Deal find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new DealNotFoundException(id));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private DealResponse toResponse(Deal deal) {
        return mapper.toResponse(deal, today());
    }

    private void applyRequest(Deal deal, DealRequest request) {
        validateChatLink(request.chatLink());
        int planDays = resolvePlanDays(request.tradeType(), request.planDays());
        LocalDate dealDate = request.dealDate() != null ? request.dealDate() : today();

        deal.setFriendName(request.friendName().trim());
        deal.setTrainerCode(blankToNull(request.trainerCode()));
        deal.setPokemonGive(request.pokemonGive().trim());
        deal.setPokemonGet(request.pokemonGet().trim());
        deal.setTradeType(request.tradeType());
        deal.setPlanDays(planDays);
        deal.setDealDate(dealDate);
        deal.setDueDate(dealDate.plusDays(planDays));
        if (request.priority() != null) {
            deal.setPriority(request.priority());
        }
        deal.setSpecialTrade(Boolean.TRUE.equals(request.specialTrade()));
        deal.setPlatform(blankToNull(request.platform()));
        deal.setChatLink(blankToNull(request.chatLink()));
        deal.setNotes(blankToNull(request.notes()));
    }

    private int resolvePlanDays(TradeType type, Integer requested) {
        if (type == TradeType.CUSTOM) {
            if (requested == null) {
                throw new InvalidDealException("planDays is required when tradeType is CUSTOM");
            }
            return requested;
        }
        return type.defaultDays();
    }

    private void validateChatLink(String link) {
        if (link == null || link.isBlank()) {
            return;
        }
        if (link.length() > MAX_LINK_LENGTH) {
            throw new InvalidDealException("chatLink must be at most " + MAX_LINK_LENGTH + " characters");
        }
        if (!link.regionMatches(true, 0, "https://", 0, "https://".length())) {
            throw new InvalidDealException("chatLink must start with https://");
        }
        try {
            URI uri = new URI(link);
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new InvalidDealException("chatLink must be a valid https:// URL");
            }
        } catch (URISyntaxException e) {
            throw new InvalidDealException("chatLink must be a valid https:// URL");
        }
    }

    private void assertSpecialTradeAvailable(Deal deal, LocalDate day) {
        boolean used = repository.existsByStatusAndSpecialTradeTrueAndCompletedOnAndIdNot(
                DealStatus.DONE, day, deal.getId());
        if (used) {
            throw new SpecialTradeAlreadyUsedException(day);
        }
    }

    private Comparator<DealResponse> resolveOrder(String sort) {
        if (sort == null || sort.isBlank()) {
            return DEFAULT_ORDER;
        }
        return switch (sort) {
            case "dueDate" -> BY_DUE_THEN_PRIORITY;
            case "priority" -> BY_PRIORITY_THEN_DUE;
            default -> throw new InvalidDealException("sort must be 'dueDate' or 'priority'");
        };
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

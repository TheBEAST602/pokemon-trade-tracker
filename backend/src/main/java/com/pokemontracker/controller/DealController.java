package com.pokemontracker.controller;

import com.pokemontracker.dto.DealRequest;
import com.pokemontracker.dto.DealResponse;
import com.pokemontracker.dto.PriorityUpdateRequest;
import com.pokemontracker.dto.StatusUpdateRequest;
import com.pokemontracker.model.DealStatus;
import com.pokemontracker.model.Priority;
import com.pokemontracker.model.TradeType;
import com.pokemontracker.service.DealService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deals")
public class DealController {

    private final DealService dealService;

    public DealController(DealService dealService) {
        this.dealService = dealService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DealResponse create(@Valid @RequestBody DealRequest request) {
        return dealService.create(request);
    }

    @GetMapping
    public List<DealResponse> list(
            @RequestParam(name = "status", required = false) DealStatus status,
            @RequestParam(name = "priority", required = false) Priority priority,
            @RequestParam(name = "tradeType", required = false) TradeType tradeType,
            @RequestParam(name = "platform", required = false) String platform,
            @RequestParam(name = "overdue", required = false) Boolean overdue,
            @RequestParam(name = "sort", required = false) String sort) {
        return dealService.list(status, priority, tradeType, platform, overdue, sort);
    }

    @GetMapping("/{id}")
    public DealResponse get(@PathVariable("id") UUID id) {
        return dealService.get(id);
    }

    @PutMapping("/{id}")
    public DealResponse update(@PathVariable("id") UUID id, @Valid @RequestBody DealRequest request) {
        return dealService.update(id, request);
    }

    @PatchMapping("/{id}/priority")
    public DealResponse updatePriority(@PathVariable("id") UUID id,
                                       @Valid @RequestBody PriorityUpdateRequest request) {
        return dealService.updatePriority(id, request.priority());
    }

    @PatchMapping("/{id}/status")
    public DealResponse updateStatus(@PathVariable("id") UUID id,
                                     @Valid @RequestBody StatusUpdateRequest request) {
        return dealService.updateStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") UUID id) {
        dealService.delete(id);
    }
}

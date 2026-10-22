package com.pokemontracker.dto;

import com.pokemontracker.model.DealStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull DealStatus status) {
}

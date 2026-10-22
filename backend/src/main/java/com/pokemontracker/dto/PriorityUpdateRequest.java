package com.pokemontracker.dto;

import com.pokemontracker.model.Priority;
import jakarta.validation.constraints.NotNull;

public record PriorityUpdateRequest(@NotNull Priority priority) {
}

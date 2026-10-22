package com.pokemontracker.dto;

import com.pokemontracker.model.Priority;
import com.pokemontracker.model.TradeType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DealRequest(
        @NotBlank @Size(max = 100) String friendName,
        @Size(max = 20) String trainerCode,
        @NotBlank @Size(max = 100) String pokemonGive,
        @NotBlank @Size(max = 100) String pokemonGet,
        @NotNull TradeType tradeType,
        @Min(0) @Max(3650) Integer planDays,
        LocalDate dealDate,
        Priority priority,
        Boolean specialTrade,
        @Size(max = 30) String platform,
        @Size(max = 500) String chatLink,
        @Size(max = 5000) String notes
) {
}

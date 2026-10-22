package com.pokemontracker.model;

public enum TradeType {
    IMMEDIATE(0),
    DAYS_7(7),
    DAYS_30(30),
    CUSTOM(null);

    private final Integer defaultDays;

    TradeType(Integer defaultDays) {
        this.defaultDays = defaultDays;
    }

    /** Default planDays for this type, or null for CUSTOM (the user must supply it). */
    public Integer defaultDays() {
        return defaultDays;
    }
}

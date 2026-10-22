package com.pokemontracker.exception;

import java.util.UUID;

public class DealNotFoundException extends RuntimeException {
    public DealNotFoundException(UUID id) {
        super("Deal not found: " + id);
    }
}

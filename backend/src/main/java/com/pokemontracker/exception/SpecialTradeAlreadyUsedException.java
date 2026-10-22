package com.pokemontracker.exception;

import java.time.LocalDate;

public class SpecialTradeAlreadyUsedException extends RuntimeException {
    public SpecialTradeAlreadyUsedException(LocalDate day) {
        super("The special trade was already used on " + day
                + ". Turn off 'Special trade' on this deal to mark it done.");
    }
}

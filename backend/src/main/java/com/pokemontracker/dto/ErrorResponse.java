package com.pokemontracker.dto;

public record ErrorResponse(int status, String error, String message) {
}

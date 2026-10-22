package com.pokemontracker.exception;

import com.pokemontracker.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DealNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(DealNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(SpecialTradeAlreadyUsedException.class)
    public ResponseEntity<ErrorResponse> handleSpecialTrade(SpecialTradeAlreadyUsedException ex) {
        return build(HttpStatus.CONFLICT, "SPECIAL_TRADE_ALREADY_USED", ex.getMessage());
    }

    @ExceptionHandler(InvalidDealException.class)
    public ResponseEntity<ErrorResponse> handleInvalid(InvalidDealException ex) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Something went wrong on the server");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return buildObject(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        return buildObject(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Request body is missing or malformed (check enum values and date format yyyy-MM-dd)");
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
                                                        HttpHeaders headers,
                                                        HttpStatusCode status,
                                                        WebRequest request) {
        String name = ex.getPropertyName() != null ? ex.getPropertyName() : "parameter";
        return buildObject(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid value for " + name);
    }

    private static ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), code, message));
    }

    private static ResponseEntity<Object> buildObject(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), code, message));
    }
}

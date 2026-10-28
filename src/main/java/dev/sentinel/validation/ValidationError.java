package dev.sentinel.validation;

public record ValidationError(int line, int column, String severity, String message) {}

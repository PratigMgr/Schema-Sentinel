package dev.sentinel.validation;

public class UnknownSchemaVersionException extends RuntimeException {
    public UnknownSchemaVersionException(String version) {
        super("Unknown schema version: " + version);
    }
}

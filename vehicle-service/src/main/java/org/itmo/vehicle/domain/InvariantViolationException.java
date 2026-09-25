package org.itmo.vehicle.domain;

public final class InvariantViolationException extends DomainException {

    private final String field;

    public InvariantViolationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}

package org.itmo.vehicle.domain;

public record Coordinates(int x, int y) {

    public static final int MAX_X = 471;

    public Coordinates {
        if (x > MAX_X) {
            throw new InvariantViolationException("coordinates.x", "must not exceed " + MAX_X);
        }
    }
}

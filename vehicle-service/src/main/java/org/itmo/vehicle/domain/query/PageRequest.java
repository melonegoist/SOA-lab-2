package org.itmo.vehicle.domain.query;

public record PageRequest(int page, int size) {

    public static final int MAX_SIZE = 100;

    public PageRequest {
        if (page < 1) {
            throw new IllegalArgumentException("page must be at least 1: " + page);
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be within 1.." + MAX_SIZE + ": " + size);
        }
    }

    public long offset() {
        return (long) (page - 1) * size;
    }
}

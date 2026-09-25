package org.itmo.vehicle.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class CoordinatesEmbeddable {

    @Column(name = "coordinate_x", nullable = false)
    private int x;

    @Column(name = "coordinate_y", nullable = false)
    private int y;

    protected CoordinatesEmbeddable() {
        // for JPA
    }

    public CoordinatesEmbeddable(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}

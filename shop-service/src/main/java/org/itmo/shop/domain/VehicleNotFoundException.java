package org.itmo.shop.domain;

public final class VehicleNotFoundException extends ShopException {

    private final int id;

    public VehicleNotFoundException(int id) {
        super("Vehicle with id " + id + " does not exist.", null);
        this.id = id;
    }

    public int getId() {
        return id;
    }
}

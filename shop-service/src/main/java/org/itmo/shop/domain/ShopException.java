package org.itmo.shop.domain;

public abstract sealed class ShopException extends RuntimeException
        permits VehicleNotFoundException, VehicleCatalogException {

    protected ShopException(String message, Throwable cause) {
        super(message, cause);
    }
}

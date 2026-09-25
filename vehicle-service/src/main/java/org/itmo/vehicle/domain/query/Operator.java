package org.itmo.vehicle.domain.query;

public enum Operator {
    EQ,
    NEQ,
    GT,
    GTE,
    LT,
    LTE,
    LIKE,
    IN,
    IS_NULL;

    public boolean isApplicableTo(VehicleField field) {
        return switch (this) {
            case EQ, NEQ, IN -> true;
            case GT, GTE, LT, LTE -> field.valueType().isOrdered();
            case LIKE -> field.valueType() == ValueType.STRING;
            case IS_NULL -> field.isNullable();
        };
    }

    public boolean isComparison() {
        return switch (this) {
            case EQ, NEQ, GT, GTE, LT, LTE -> true;
            case LIKE, IN, IS_NULL -> false;
        };
    }
}

package org.itmo.vehicle.domain.query;

import java.util.List;
import java.util.Objects;

public sealed interface Criterion permits Criterion.Comparison, Criterion.Like, Criterion.In, Criterion.IsNull {

    VehicleField field();

    /**
     * eq, neq, gt, gte, lt, lte.
     */
    record Comparison(VehicleField field, Operator operator, Object value) implements Criterion {
        public Comparison {
            if (!operator.isComparison()) {
                throw new IllegalArgumentException(operator + " is not a comparison");
            }
            requireApplicable(operator, field);
            requireValueOf(field, value);
        }
    }

    record Like(VehicleField field, String fragment) implements Criterion {
        public Like {
            requireApplicable(Operator.LIKE, field);
            Objects.requireNonNull(fragment, "fragment");
        }
    }

    record In(VehicleField field, List<Object> values) implements Criterion {
        public In {
            values = List.copyOf(values);
            if (values.isEmpty()) {
                throw new IllegalArgumentException("in requires at least one value");
            }
            values.forEach(value -> requireValueOf(field, value));
        }
    }

    record IsNull(VehicleField field, boolean isNull) implements Criterion {
        public IsNull {
            requireApplicable(Operator.IS_NULL, field);
        }
    }

    private static void requireApplicable(Operator operator, VehicleField field) {
        if (!operator.isApplicableTo(field)) {
            throw new IllegalArgumentException(operator + " does not apply to " + field.path());
        }
    }

    private static void requireValueOf(VehicleField field, Object value) {
        if (!field.valueType().javaType().isInstance(value)) {
            throw new IllegalArgumentException(field.path() + " expects " + field.valueType()
                    + ", got " + (value == null ? "null" : value.getClass().getSimpleName()));
        }
    }
}

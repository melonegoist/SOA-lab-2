package org.itmo.vehicle.infrastructure.persistence;

import jakarta.persistence.criteria.*;
import org.itmo.vehicle.domain.query.Criterion;
import org.itmo.vehicle.domain.query.SortKey;
import org.itmo.vehicle.domain.query.VehicleField;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;

final class CriteriaTranslator {

    private static final char LIKE_ESCAPE = '\\';

    private final CriteriaBuilder builder;
    private final Root<VehicleEntity> root;

    CriteriaTranslator(CriteriaBuilder builder, Root<VehicleEntity> root) {
        this.builder = builder;
        this.root = root;
    }

    Predicate[] predicates(List<Criterion> criteria) {
        return criteria.stream().map(this::predicate).toArray(Predicate[]::new);
    }

    List<Order> orders(List<SortKey> keys) {
        return keys.stream()
                .map(key -> key.direction() == SortKey.Direction.ASCENDING
                        ? builder.asc(path(key.field()))
                        : builder.desc(path(key.field())))
                .toList();
    }

    private Predicate predicate(Criterion criterion) {
        if (criterion instanceof Criterion.Comparison comparison) {
            return comparison(comparison);
        }
        if (criterion instanceof Criterion.Like like) {
            String pattern = "%" + escapeLike(like.fragment().toLowerCase(Locale.ROOT)) + "%";
            return builder.like(builder.lower(this.<String>path(like.field())), pattern, LIKE_ESCAPE);
        }
        if (criterion instanceof Criterion.In in) {
            return path(in.field()).in(in.values().stream().map(CriteriaTranslator::toColumnValue).toList());
        }
        if (criterion instanceof Criterion.IsNull isNull) {
            Path<Object> path = path(isNull.field());
            return isNull.isNull() ? builder.isNull(path) : builder.isNotNull(path);
        }
        throw new IllegalArgumentException("Unsupported criterion: " + criterion);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate comparison(Criterion.Comparison comparison) {
        Expression path = path(comparison.field());
        Comparable value = (Comparable) toColumnValue(comparison.value());
        return switch (comparison.operator()) {
            case EQ -> builder.equal(path, value);
            case NEQ -> builder.notEqual(path, value);
            case GT -> builder.greaterThan(path, value);
            case GTE -> builder.greaterThanOrEqualTo(path, value);
            case LT -> builder.lessThan(path, value);
            case LTE -> builder.lessThanOrEqualTo(path, value);
            case LIKE, IN, IS_NULL ->
                    throw new IllegalArgumentException(comparison.operator() + " is not a comparison");
        };
    }

    private <T> Path<T> path(VehicleField field) {
        return switch (field) {
            case ID -> root.get("id");
            case NAME -> root.get("name");
            case COORDINATES_X -> root.get("coordinates").get("x");
            case COORDINATES_Y -> root.get("coordinates").get("y");
            case CREATION_DATE -> root.get("creationDate");
            case ENGINE_POWER -> root.get("enginePower");
            case NUMBER_OF_WHEELS -> root.get("numberOfWheels");
            case MILEAGE -> root.get("mileage");
            case TYPE -> root.get("type");
            case FUEL_TYPE -> root.get("fuelType");
        };
    }

    private static Object toColumnValue(Object value) {
        if (value instanceof ZonedDateTime dateTime) {
            return dateTime.toOffsetDateTime();
        }
        if (value instanceof Float number) {
            return number.doubleValue();
        }
        return value;
    }

    private static String escapeLike(String fragment) {
        return fragment
                .replace(String.valueOf(LIKE_ESCAPE), "" + LIKE_ESCAPE + LIKE_ESCAPE)
                .replace("%", LIKE_ESCAPE + "%")
                .replace("_", LIKE_ESCAPE + "_");
    }
}

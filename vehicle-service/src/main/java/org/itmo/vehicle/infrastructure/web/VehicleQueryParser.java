package org.itmo.vehicle.infrastructure.web;

import jakarta.enterprise.context.ApplicationScoped;
import org.itmo.vehicle.domain.FuelType;
import org.itmo.vehicle.domain.VehicleType;
import org.itmo.vehicle.domain.query.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ApplicationScoped
public class VehicleQueryParser {

    private static final Map<String, Operator> OPERATORS = Map.of(
            "eq", Operator.EQ,
            "neq", Operator.NEQ,
            "gt", Operator.GT,
            "gte", Operator.GTE,
            "lt", Operator.LT,
            "lte", Operator.LTE,
            "like", Operator.LIKE,
            "in", Operator.IN,
            "isnull", Operator.IS_NULL);

    private static final Pattern INTEGER = Pattern.compile("-?(0|[1-9]\\d*)");
    private static final Pattern NUMBER = Pattern.compile("-?(0|[1-9]\\d*)(\\.\\d+)?([eE][+-]?\\d+)?");

    public VehicleQuery parse(List<String> filters, List<String> sorts, int page, int size) {
        List<Criterion> criteria = new ArrayList<>();
        List<String> filterValues = orEmpty(filters);
        for (int i = 0; i < filterValues.size(); i++) {
            criteria.add(parseFilter(filterValues.get(i), "filter/" + i));
        }

        List<SortKey> keys = new ArrayList<>();
        Set<VehicleField> sorted = new HashSet<>();
        List<String> sortValues = orEmpty(sorts);
        for (int i = 0; i < sortValues.size(); i++) {
            SortKey key = parseSortKey(sortValues.get(i), "sort/" + i);
            if (!sorted.add(key.field())) {
                throw new InvalidParameterException("sort/" + i,
                        "sorts by '" + key.field().path() + "' a second time");
            }
            keys.add(key);
        }
        return new VehicleQuery(criteria, keys, new PageRequest(page, size));
    }

    private Criterion parseFilter(String filter, String pointer) {
        String[] parts = filter.split(":", 3);
        if (parts.length < 3 || parts[2].isEmpty()) {
            throw new InvalidParameterException(pointer, "must have the form field:operator:value");
        }
        VehicleField field = VehicleField.byPath(parts[0]).orElseThrow(() ->
                new InvalidParameterException(pointer, "unknown field '" + parts[0] + "'"));
        Operator operator = OPERATORS.get(parts[1]);
        if (operator == null) {
            throw new InvalidParameterException(pointer, "unknown operator '" + parts[1] + "'");
        }
        if (!operator.isApplicableTo(field)) {
            throw new InvalidParameterException(pointer,
                    "operator '" + parts[1] + "' does not apply to field '" + field.path() + "'");
        }
        String value = parts[2];
        return switch (operator) {
            case IS_NULL -> new Criterion.IsNull(field, parseBoolean(value, pointer));
            case LIKE -> new Criterion.Like(field, value);
            case IN -> new Criterion.In(field, parseList(field, value, pointer));
            case EQ, NEQ, GT, GTE, LT, LTE ->
                    new Criterion.Comparison(field, operator, parseValue(field, value, pointer));
        };
    }

    private SortKey parseSortKey(String sort, String pointer) {
        boolean descending = sort.startsWith("-");
        String path = descending ? sort.substring(1) : sort;
        VehicleField field = VehicleField.byPath(path).orElseThrow(() ->
                new InvalidParameterException(pointer, "unknown field '" + path + "'"));
        return descending ? SortKey.descending(field) : SortKey.ascending(field);
    }

    private static boolean parseBoolean(String value, String pointer) {
        return switch (value) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new InvalidParameterException(pointer, "isnull expects true or false");
        };
    }

    private static List<Object> parseList(VehicleField field, String value, String pointer) {
        String[] items = value.split(",", -1);
        List<Object> values = new ArrayList<>(items.length);
        for (String item : items) {
            if (item.isEmpty()) {
                throw new InvalidParameterException(pointer, "in expects a comma-separated list without empty items");
            }
            values.add(parseValue(field, item, pointer));
        }
        return values;
    }

    private static Object parseValue(VehicleField field, String value, String pointer) {
        return switch (field.valueType()) {
            case INTEGER -> parseInteger(value, pointer);
            case FLOAT -> parseFloat(value, pointer);
            case DOUBLE -> parseDouble(value, pointer);
            case DATE_TIME -> parseDateTime(value, pointer);
            case STRING -> value;
            case VEHICLE_TYPE -> parseEnum(VehicleType.class, value, pointer);
            case FUEL_TYPE -> parseEnum(FuelType.class, value, pointer);
        };
    }

    private static Integer parseInteger(String value, String pointer) {
        if (!INTEGER.matcher(value).matches()) {
            throw new InvalidParameterException(pointer, "'" + value + "' is not an integer");
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            throw new InvalidParameterException(pointer, "'" + value + "' is out of the int32 range");
        }
    }

    private static Float parseFloat(String value, String pointer) {
        requireNumber(value, pointer);
        float number = Float.parseFloat(value);
        if (Float.isInfinite(number)) {
            throw new InvalidParameterException(pointer, "'" + value + "' is out of the float range");
        }
        return number;
    }

    private static Double parseDouble(String value, String pointer) {
        requireNumber(value, pointer);
        double number = Double.parseDouble(value);
        if (Double.isInfinite(number)) {
            throw new InvalidParameterException(pointer, "'" + value + "' is out of the double range");
        }
        return number;
    }

    private static void requireNumber(String value, String pointer) {
        if (!NUMBER.matcher(value).matches()) {
            throw new InvalidParameterException(pointer, "'" + value + "' is not a number");
        }
    }

    private static Object parseDateTime(String value, String pointer) {
        try {
            return OffsetDateTime.parse(value).toZonedDateTime();
        } catch (DateTimeParseException e) {
            throw new InvalidParameterException(pointer, "'" + value + "' is not an RFC 3339 date-time, "
                    + "e.g. 2026-09-10T08:19:00+03:00 (in a URL, + must be written as %2B)");
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String pointer) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            String allowed = Arrays.stream(type.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
            throw new InvalidParameterException(pointer, "'" + value + "' is not one of " + allowed);
        }
    }

    private static List<String> orEmpty(List<String> values) {
        return values == null ? List.of() : values;
    }
}

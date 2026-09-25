package org.itmo.vehicle.domain.query;

import java.util.*;

public record VehicleQuery(List<Criterion> criteria, List<SortKey> sort, PageRequest page) {

    public VehicleQuery {
        criteria = List.copyOf(criteria);
        sort = List.copyOf(sort);
        Objects.requireNonNull(page, "page");
        Set<VehicleField> sortFields = new HashSet<>();
        for (SortKey key : sort) {
            if (!sortFields.add(key.field())) {
                throw new IllegalArgumentException("duplicate sort key: " + key.field().path());
            }
        }
    }

    public List<SortKey> effectiveSort() {
        boolean sortedById = sort.stream().anyMatch(key -> key.field() == VehicleField.ID);
        if (sortedById) {
            return sort;
        }
        List<SortKey> keys = new ArrayList<>(sort);
        keys.add(SortKey.ascending(VehicleField.ID));
        return List.copyOf(keys);
    }
}

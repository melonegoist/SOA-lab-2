package org.itmo.vehicle.domain.query;

import java.util.List;
import java.util.function.Function;

public record Page<T>(List<T> items, int page, int size, long totalElements) {

    public Page {
        items = List.copyOf(items);
    }

    public static <T> Page<T> empty(PageRequest request, long totalElements) {
        return new Page<>(List.of(), request.page(), request.size(), totalElements);
    }

    public int totalPages() {
        return Math.toIntExact((totalElements + size - 1) / size);
    }

    public <R> Page<R> map(Function<? super T, ? extends R> mapper) {
        return new Page<>(items.stream().<R>map(mapper).toList(), page, size, totalElements);
    }
}

package com.tripvisito.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic paginated response wrapper.
 *
 * <p>Mirrors the original Express pagination shape used across trip,
 * user, and review listing endpoints:
 * <pre>
 * {
 *   "items": [...],
 *   "totalPages": 5,
 *   "totalCount": 48,
 *   "page": 1
 * }
 * </pre>
 *
 * @param <T> the type of items in the list
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {

    private List<T> items;
    private int totalPages;
    private long totalCount;
    private int page;

    public static <T> PagedResponse<T> of(List<T> items, long totalCount, int page, int limit) {
        return PagedResponse.<T>builder()
                .items(items)
                .totalCount(totalCount)
                .totalPages((int) Math.ceil((double) totalCount / limit))
                .page(page)
                .build();
    }
}

package com.pnis.backend.common.util;

import com.pnis.backend.common.api.ApiResponse;
import org.springframework.data.domain.Page;

public final class PageUtils {

    private PageUtils() {}

    public static <T> ApiResponse<java.util.List<T>> toPagedResponse(Page<T> page) {
        ApiResponse.PageMeta meta = new ApiResponse.PageMeta(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
        return ApiResponse.paged(page.getContent(), meta);
    }
}

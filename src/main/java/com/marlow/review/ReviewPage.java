package com.marlow.review;

import java.util.List;

public record ReviewPage(
        List<ReviewResponse> items,
        int page,
        int size,
        long total
) {
}

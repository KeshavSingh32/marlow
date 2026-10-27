package com.marlow.review;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviews;

    public ReviewController(ReviewService reviews) {
        this.reviews = reviews;
    }

    @GetMapping
    public ReviewPage list(@RequestParam(required = false) Sentiment sentiment,
                           @RequestParam(required = false) ReviewCategory category,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "12") int size) {
        return reviews.list(sentiment, category, page, size);
    }

    @PostMapping
    public ReviewResponse create(@Valid @RequestBody CreateReviewRequest request) {
        return reviews.ingest(request);
    }

    @PostMapping("/{id}/redraft")
    public ReviewResponse redraft(@PathVariable UUID id, @RequestParam(defaultValue = "brief") String tone) {
        return reviews.redraft(id, tone);
    }

    @PatchMapping("/{id}")
    public ReviewResponse update(@PathVariable UUID id, @RequestBody UpdateReplyRequest request) {
        return reviews.updateReply(id, request);
    }
}

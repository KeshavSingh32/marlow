package com.marlow.intelligence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelReply(
        String sentiment,
        Double score,
        String category,
        List<String> themes,
        String draftReply
) {
}

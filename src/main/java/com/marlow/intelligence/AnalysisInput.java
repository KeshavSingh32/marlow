package com.marlow.intelligence;

public record AnalysisInput(
        String businessName,
        String industry,
        String author,
        Integer rating,
        String source,
        String text,
        String tone
) {
    public AnalysisInput {
        if (tone == null || tone.isBlank()) {
            tone = "warm";
        }
    }
}

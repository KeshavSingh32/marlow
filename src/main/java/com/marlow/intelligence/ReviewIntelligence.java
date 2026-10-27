package com.marlow.intelligence;

public interface ReviewIntelligence {

    ReviewInsight analyze(AnalysisInput input);

    String mode();
}

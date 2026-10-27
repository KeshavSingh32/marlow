package com.marlow.batchjob;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReviewCsvRow {
    private String author;
    private String rating;
    private String source;
    private String reviewedAt;
    private String text;
}

package com.marlow.batchjob;

import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Path;
import java.util.Arrays;

public final class ReviewCsvReaderFactory {

    private ReviewCsvReaderFactory() {
    }

    public static FlatFileItemReader<ReviewCsvRow> open(String filePath) throws Exception {
        Path path = Path.of(filePath);
        String[] names = CsvColumns.namesFor(path);
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames(names);
        tokenizer.setStrict(false);
        DefaultLineMapper<ReviewCsvRow> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(ReviewCsvReaderFactory::map);
        lineMapper.afterPropertiesSet();

        FlatFileItemReader<ReviewCsvRow> reader = new FlatFileItemReader<>();
        reader.setName("reviewCsvReader");
        reader.setResource(new FileSystemResource(path));
        reader.setLinesToSkip(1);
        reader.setEncoding("UTF-8");
        reader.setLineMapper(lineMapper);
        return reader;
    }

    private static ReviewCsvRow map(FieldSet fieldSet) {
        ReviewCsvRow row = new ReviewCsvRow();
        row.setAuthor(read(fieldSet, "author"));
        row.setRating(read(fieldSet, "rating"));
        row.setSource(read(fieldSet, "source"));
        row.setReviewedAt(read(fieldSet, "reviewedAt"));
        row.setText(read(fieldSet, "text"));
        return row;
    }

    private static String read(FieldSet fieldSet, String name) {
        if (!Arrays.asList(fieldSet.getNames()).contains(name)) {
            return null;
        }
        String value = fieldSet.readString(name);
        return value == null || value.isBlank() ? null : value.trim();
    }
}

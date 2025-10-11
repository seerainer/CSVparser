package io.github.seerainer.csv.demo;

import java.security.SecureRandom;

import io.github.seerainer.csv.CSVConfiguration;
import io.github.seerainer.csv.CSVParseException;
import io.github.seerainer.csv.CSVParser;
import io.github.seerainer.csv.CSVParsingOptions;

public class Benchmark {

    private static String generateTestData(final int rows, final int columns) {
	final var sb = new StringBuilder();
	final var random = new SecureRandom();
	random.setSeed(42); // Fixed seed for reproducibility

	// Header
	for (var j = 0; j < columns; j++) {
	    if (j > 0) {
		sb.append(',');
	    }
	    sb.append("Column").append(j + 1);
	}
	sb.append('\n');

	// Data rows
	for (var i = 0; i < rows; i++) {
	    for (var j = 0; j < columns; j++) {
		if (j > 0) {
		    sb.append(',');
		}

		if (random.nextBoolean()) {
		    // Sometimes add quotes
		    sb.append('"').append("Value_").append(i).append('_').append(j);
		    if (random.nextInt(10) == 0) {
			sb.append(" with \"\"quotes\"\"");
		    }
		    sb.append('"');
		} else {
		    sb.append("Value_").append(i).append('_').append(j);
		}
	    }
	    sb.append('\n');
	}

	return sb.toString();
    }

    public static void main(final String[] args) {
	final var config = CSVConfiguration.builder().delimiter(',').quote('"').trimWhitespace(true)
		.initialBufferSize(2048).build();

	final var preserveOptions = CSVParsingOptions.builder().build();

	final var parser = new CSVParser(config, preserveOptions);

	// Generate test data
	final var testData = generateTestData(10000, 10);

	System.out.println(new StringBuilder().append("Generated CSV with ~").append(testData.length())
		.append(" characters").toString());

	// Warm up JVM
	for (var i = 0; i < 5; i++) {
	    try {
		parser.parseByteArray(testData.getBytes());
	    } catch (final CSVParseException e) {
		e.printStackTrace();
	    }
	}

	// Benchmark
	final var startTime = System.nanoTime();
	final var iterations = 100;

	for (var i = 0; i < iterations; i++) {
	    try {
		final var records = parser.parseByteArray(testData.getBytes());
		if (i == 0) {
		    System.out.println(
			    new StringBuilder().append("Parsed ").append(records.size()).append(" records").toString());
		}
	    } catch (final CSVParseException e) {
		e.printStackTrace();
	    }
	}

	final var endTime = System.nanoTime();
	final var totalTime = endTime - startTime;
	final var avgTime = totalTime / (double) iterations / 1_000_000; // Convert to milliseconds

	System.out.printf("Average parsing time: %.2f ms%n", Double.valueOf(avgTime));
	System.out.printf("Throughput: %.2f MB/s%n",
		Double.valueOf((testData.length() / 1024.0 / 1024.0) / (avgTime / 1000.0)));
    }
}
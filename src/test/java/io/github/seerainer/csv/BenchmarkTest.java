package io.github.seerainer.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;

import org.junit.jupiter.api.Test;

/**
 * Performance benchmark tests for CSVParser
 */
public class BenchmarkTest {

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

    @SuppressWarnings("static-method")
    @Test
    public void testLargeDatasetParsing() throws CSVParseException {
	final var config = CSVConfiguration.builder().delimiter(',').quote('"').trimWhitespace(true)
		.initialBufferSize(4096).build();

	final var options = CSVParsingOptions.builder().build();
	final var parser = new CSVParser(config, options);

	// Test with larger dataset
	final var testData = generateTestData(50000, 20);
	final var records = parser.parseByteArray(testData.getBytes());

	assertNotNull(records, "Records should not be null");
	assertEquals(50001, records.size(), "Should have header + 50000 data rows");

	// Verify some random records
	final var firstDataRow = records.get(1);
	assertEquals(20, firstDataRow.getFieldCount(), "Each row should have 20 fields");
    }

    @SuppressWarnings("static-method")
    @Test
    public void testParserCorrectness() throws CSVParseException {
	final var config = CSVConfiguration.builder().delimiter(',').quote('"').trimWhitespace(true)
		.initialBufferSize(2048).build();

	final var preserveOptions = CSVParsingOptions.builder().build();
	final var parser = new CSVParser(config, preserveOptions);

	// Generate small test data
	final var testData = generateTestData(100, 5);
	final var records = parser.parseByteArray(testData.getBytes());

	// Verify parsing correctness
	assertNotNull(records, "Records should not be null");
	assertEquals(101, records.size(), "Should have header + 100 data rows");

	// Verify header
	final var header = records.get(0);
	assertEquals(5, header.getFieldCount(), "Header should have 5 columns");
	assertEquals("Column1", header.getField(0), "First column should be Column1");
	assertEquals("Column5", header.getField(4), "Last column should be Column5");
    }

    @SuppressWarnings("static-method")
    @Test
    public void testParserPerformance() throws CSVParseException {
	final var config = CSVConfiguration.builder().delimiter(',').quote('"').trimWhitespace(true)
		.initialBufferSize(2048).build();

	final var preserveOptions = CSVParsingOptions.builder().build();

	final var parser = new CSVParser(config, preserveOptions);

	// Generate test data
	final var testData = generateTestData(10000, 10);

	assertTrue(testData.length() > 0, "Generated CSV should have content");
	System.out.println(new StringBuilder().append("Generated CSV with ~").append(testData.length())
		.append(" characters").toString());

	// Warm up JVM
	for (var i = 0; i < 5; i++) {
	    parser.parseByteArray(testData.getBytes());
	}

	// Benchmark
	final var startTime = System.nanoTime();
	final var iterations = 100;

	for (var i = 0; i < iterations; i++) {
	    final var records = parser.parseByteArray(testData.getBytes());
	    if (i == 0) {
		assertNotNull(records, "Records should not be null");
		assertTrue(records.size() > 0, "Should parse at least one record");
		System.out.println(
			new StringBuilder().append("Parsed ").append(records.size()).append(" records").toString());
	    }
	}

	final var endTime = System.nanoTime();
	final var totalTime = endTime - startTime;
	final var avgTime = totalTime / (double) iterations / 1_000_000; // Convert to milliseconds

	System.out.printf("Average parsing time: %.2f ms%n", Double.valueOf(avgTime));
	System.out.printf("Throughput: %.2f MB/s%n",
		Double.valueOf((testData.length() / 1024.0 / 1024.0) / (avgTime / 1000.0)));

	// Assert reasonable performance (adjust threshold as needed)
	assertTrue(avgTime < 1000, "Average parsing time should be less than 1 second");
    }
}

package io.github.seerainer.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Test suite for buffer expansion safety in CSVParser Verifies that fields
 * exceeding maxFieldSize throw appropriate exceptions
 */
public class CSVParserBufferSafetyTest {

    @SuppressWarnings("static-method")
    @Test
    public void testBufferExpandsCorrectlyUpToLimit() throws CSVParseException {
	final var config = CSVConfiguration.builder().maxFieldSize(1000).initialBufferSize(10) // Start very small
		.build();
	final var options = CSVParsingOptions.builder().build();
	final var parser = new CSVParser(config, options);

	// Field that requires multiple expansions but stays within limit
	final var field = "x".repeat(500);
	final var data = field.getBytes(StandardCharsets.UTF_8);

	final var records = parser.parseByteArray(data);

	assertEquals(1, records.size(), "Should parse one record");
	assertEquals(500, records.get(0).getField(0).length(), "Field should be 500 characters");
    }

    @SuppressWarnings("static-method")
    @Test
    public void testDefaultMaxFieldSizeIsGenerous() throws CSVParseException {
	// Use default configuration (max field size is 1MB by default)
	final var config = CSVConfiguration.builder().build();
	final var options = CSVParsingOptions.builder().build();
	final var parser = new CSVParser(config, options);

	// Create a reasonably large field (10KB)
	final var field = "x".repeat(10000);
	final var data = field.getBytes(StandardCharsets.UTF_8);

	final var records = parser.parseByteArray(data);

	assertEquals(1, records.size(), "Should parse one record");
	assertEquals(10000, records.get(0).getField(0).length(), "Field should be 10000 characters");
    }

    @SuppressWarnings("static-method")
    @Test
    public void testFieldAtMaxSizeIsAllowed() throws CSVParseException {
	// Create a max field size
	final var config = CSVConfiguration.builder().maxFieldSize(100).initialBufferSize(10).build();
	final var options = CSVParsingOptions.builder().build();
	final var parser = new CSVParser(config, options);

	// Create a field exactly at the max size (should be allowed)
	final var field = "x".repeat(100);
	final var data = field.getBytes(StandardCharsets.UTF_8);

	final var records = parser.parseByteArray(data);

	assertEquals(1, records.size(), "Should parse one record");
	assertEquals(100, records.get(0).getField(0).length(), "Field should be exactly 100 characters");
    }

    @SuppressWarnings("static-method")
    @Test
    public void testFieldExceedsMaxSize() {
	// Create a small max field size for testing
	final var config = CSVConfiguration.builder().maxFieldSize(100).initialBufferSize(10).build();
	final var options = CSVParsingOptions.builder().build();
	final var parser = new CSVParser(config, options);

	// Create a field that exceeds the max size
	final var largeField = "x".repeat(150);
	final var data = largeField.getBytes(StandardCharsets.UTF_8);

	// Should throw IllegalStateException when buffer expansion fails
	final var exception = assertThrows(IllegalStateException.class, () -> parser.parseByteArray(data));

	assertTrue(exception.getMessage().contains("Field size exceeds maximum allowed size"),
		"Exception message should mention field size limit");
	assertTrue(exception.getMessage().contains("100"), "Exception message should include the max size value");
    }

    @SuppressWarnings("static-method")
    @Test
    public void testFieldJustUnderMaxSizeIsAllowed() throws CSVParseException {
	final var config = CSVConfiguration.builder().maxFieldSize(100).initialBufferSize(10).build();
	final var options = CSVParsingOptions.builder().build();
	final var parser = new CSVParser(config, options);

	// Create a field just under the max size
	final var field = "x".repeat(99);
	final var data = field.getBytes(StandardCharsets.UTF_8);

	final var records = parser.parseByteArray(data);

	assertEquals(1, records.size(), "Should parse one record");
	assertEquals(99, records.get(0).getField(0).length(), "Field should be 99 characters");
    }

    @SuppressWarnings("static-method")
    @Test
    public void testMultipleFieldsWithinLimit() throws CSVParseException {
	final var config = CSVConfiguration.builder().maxFieldSize(50).initialBufferSize(10).build();
	final var options = CSVParsingOptions.builder().build();
	final var parser = new CSVParser(config, options);

	// Multiple fields, each within limit
	final var field1 = "a".repeat(40);
	final var field2 = "b".repeat(45);
	final var field3 = "c".repeat(30);
	final var data = (new StringBuilder().append(field1).append(",").append(field2).append(",").append(field3)
		.toString()).getBytes(StandardCharsets.UTF_8);

	final var records = parser.parseByteArray(data);

	assertEquals(1, records.size(), "Should parse one record");
	assertEquals(3, records.get(0).getFieldCount(), "Should have 3 fields");
	assertEquals(40, records.get(0).getField(0).length());
	assertEquals(45, records.get(0).getField(1).length());
	assertEquals(30, records.get(0).getField(2).length());
    }

    @SuppressWarnings("static-method")
    @Test
    public void testOneFieldExceedsLimitInMultiFieldRecord() {
	final var config = CSVConfiguration.builder().maxFieldSize(50).initialBufferSize(10).build();
	final var options = CSVParsingOptions.builder().build();
	final var parser = new CSVParser(config, options);

	// Second field exceeds limit
	final var field1 = "a".repeat(40);
	final var field2 = "b".repeat(60); // Exceeds limit
	final var field3 = "c".repeat(30);
	final var data = (new StringBuilder().append(field1).append(",").append(field2).append(",").append(field3)
		.toString()).getBytes(StandardCharsets.UTF_8);

	assertThrows(IllegalStateException.class, () -> parser.parseByteArray(data));
    }

    @SuppressWarnings("static-method")
    @Test
    public void testQuotedFieldExceedsMaxSize() {
	final var config = CSVConfiguration.builder().maxFieldSize(50).initialBufferSize(10).build();
	final var options = CSVParsingOptions.builder().build();
	final var parser = new CSVParser(config, options);

	// Quoted field that exceeds limit
	final var field = "x".repeat(60);
	final var data = (new StringBuilder().append("\"").append(field).append("\"").toString())
		.getBytes(StandardCharsets.UTF_8);

	assertThrows(IllegalStateException.class, () -> parser.parseByteArray(data));
    }
}

package io.github.seerainer.csv;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test suite for EOF handling in CSVParser Tests that final fields/records are
 * properly captured when input ends without trailing newline
 */
public class CSVParserEOFTest {

    private CSVConfiguration config;
    private CSVParsingOptions options;

    @BeforeEach
    public void setup() {
	config = CSVConfiguration.builder().build();
	options = CSVParsingOptions.builder().build();
    }

    @Test
    public void testCRLFEndingVsNoCRLF() throws CSVParseException {
	final var parser = new CSVParser(config, options);

	final var dataWithCRLF = "a,b,c\r\n".getBytes(StandardCharsets.UTF_8);
	final var recordsWithCRLF = parser.parseByteArray(dataWithCRLF);

	final var dataWithoutCRLF = "a,b,c".getBytes(StandardCharsets.UTF_8);
	final var recordsWithoutCRLF = parser.parseByteArray(dataWithoutCRLF);

	assertEquals(recordsWithCRLF.size(), recordsWithoutCRLF.size(), "Should parse same number of records");
	assertEquals(recordsWithCRLF.get(0).getFieldCount(), recordsWithoutCRLF.get(0).getFieldCount(),
		"Should have same field count");
	assertArrayEquals(recordsWithCRLF.get(0).getFields(), recordsWithoutCRLF.get(0).getFields(),
		"Field values should match");
    }

    @Test
    public void testEmptyFieldsWithoutNewline() throws CSVParseException {
	final var parser = new CSVParser(config, options);
	final var data = "a,,c".getBytes(StandardCharsets.UTF_8);

	final var records = parser.parseByteArray(data);

	assertEquals(1, records.size(), "Should parse one record");
	final var record = records.get(0);
	assertEquals(3, record.getFieldCount(), "Should have 3 fields");
	assertEquals("a", record.getField(0));
	assertEquals("", record.getField(1), "Middle field should be empty");
	assertEquals("c", record.getField(2));
    }

    @Test
    public void testMultipleRecordsLastWithoutNewline() throws CSVParseException {
	final var parser = new CSVParser(config, options);
	final var data = "a,b,c\nx,y,z".getBytes(StandardCharsets.UTF_8);

	final var records = parser.parseByteArray(data);

	assertEquals(2, records.size(), "Should parse two records");

	final var record1 = records.get(0);
	assertEquals(3, record1.getFieldCount(), "First record should have 3 fields");
	assertEquals("a", record1.getField(0));
	assertEquals("b", record1.getField(1));
	assertEquals("c", record1.getField(2));

	final var record2 = records.get(1);
	assertEquals(3, record2.getFieldCount(), "Second record should have 3 fields");
	assertEquals("x", record2.getField(0));
	assertEquals("y", record2.getField(1));
	assertEquals("z", record2.getField(2));
    }

    @Test
    public void testMultipleRecordsWithMixedNewlines() throws CSVParseException {
	final var parser = new CSVParser(config, options);
	final var data = "a,b,c\nx,y,z\nfoo,bar,baz".getBytes(StandardCharsets.UTF_8);

	final var records = parser.parseByteArray(data);

	assertEquals(3, records.size(), "Should parse three records");
	assertEquals("baz", records.get(2).getField(2), "Last field of last record should be captured");
    }

    @Test
    public void testPreserveEmptyFieldsWithTrailingDelimiterNoNewline() throws CSVParseException {
	final var customOptions = CSVParsingOptions.builder().preserveEmptyFields(true).build();
	final var parser = new CSVParser(config, customOptions);
	final var data = "a,b,c,".getBytes(StandardCharsets.UTF_8);

	final var records = parser.parseByteArray(data);

	assertEquals(1, records.size(), "Should parse one record");
	final var record = records.get(0);
	assertEquals(4, record.getFieldCount(), "Should have 4 fields with trailing empty");
	assertEquals("", record.getField(3), "Last field should be empty");
    }

    @Test
    public void testQuotedFieldWithoutNewline() throws CSVParseException {
	final var parser = new CSVParser(config, options);
	final var data = "a,\"b\",c".getBytes(StandardCharsets.UTF_8);

	final var records = parser.parseByteArray(data);

	assertEquals(1, records.size(), "Should parse one record");
	final var record = records.get(0);
	assertEquals(3, record.getFieldCount(), "Should have 3 fields");
	assertEquals("a", record.getField(0));
	assertEquals("b", record.getField(1));
	assertTrue(record.getFieldInfo(1).wasQuoted(), "Middle field should be marked as quoted");
	assertEquals("c", record.getField(2));
    }

    @Test
    public void testSimpleRecordWithoutTrailingNewline() throws CSVParseException {
	final var parser = new CSVParser(config, options);
	final var data = "a,b,c".getBytes(StandardCharsets.UTF_8);

	final var records = parser.parseByteArray(data);

	assertEquals(1, records.size(), "Should parse one record");
	final var record = records.get(0);
	assertEquals(3, record.getFieldCount(), "Should have 3 fields");
	assertEquals("a", record.getField(0));
	assertEquals("b", record.getField(1));
	assertEquals("c", record.getField(2));
    }

    @Test
    public void testSimpleRecordWithTrailingNewline() throws CSVParseException {
	final var parser = new CSVParser(config, options);
	final var data = "a,b,c\n".getBytes(StandardCharsets.UTF_8);

	final var records = parser.parseByteArray(data);

	assertEquals(1, records.size(), "Should parse one record");
	final var record = records.get(0);
	assertEquals(3, record.getFieldCount(), "Should have 3 fields");
	assertEquals("a", record.getField(0));
	assertEquals("b", record.getField(1));
	assertEquals("c", record.getField(2));
    }

    @Test
    public void testSingleFieldWithoutNewline() throws CSVParseException {
	final var parser = new CSVParser(config, options);
	final var data = "hello".getBytes(StandardCharsets.UTF_8);

	final var records = parser.parseByteArray(data);

	assertEquals(1, records.size(), "Should parse one record");
	final var record = records.get(0);
	assertEquals(1, record.getFieldCount(), "Should have 1 field");
	assertEquals("hello", record.getField(0));
    }

    @Test
    public void testTrailingDelimiterWithNewline() throws CSVParseException {
	final var parser = new CSVParser(config, options);
	final var data = "a,b,\n".getBytes(StandardCharsets.UTF_8);

	final var records = parser.parseByteArray(data);

	assertEquals(1, records.size(), "Should parse one record");
	final var record = records.get(0);
	assertEquals(3, record.getFieldCount(), "Should have 3 fields including trailing empty");
	assertEquals("a", record.getField(0));
	assertEquals("b", record.getField(1));
	assertEquals("", record.getField(2), "Trailing delimiter should create empty field");
    }

    @Test
    public void testTrailingDelimiterWithoutNewline() throws CSVParseException {
	final var parser = new CSVParser(config, options);
	final var data = "a,b,".getBytes(StandardCharsets.UTF_8);

	final var records = parser.parseByteArray(data);

	assertEquals(1, records.size(), "Should parse one record");
	final var record = records.get(0);
	assertEquals(3, record.getFieldCount(), "Should have 3 fields including trailing empty");
	assertEquals("a", record.getField(0));
	assertEquals("b", record.getField(1));
	assertEquals("", record.getField(2), "Trailing delimiter should create empty field");
    }

    @Test
    public void testWhitespaceOnlyWithoutNewline() throws CSVParseException {
	final var customConfig = CSVConfiguration.builder().trimWhitespace(false).build();
	final var parser = new CSVParser(customConfig, options);
	final var data = "  ,  ,  ".getBytes(StandardCharsets.UTF_8);

	final var records = parser.parseByteArray(data);

	assertEquals(1, records.size(), "Should parse one record");
	final var record = records.get(0);
	assertEquals(3, record.getFieldCount(), "Should have 3 fields");
	assertEquals("  ", record.getField(0));
	assertEquals("  ", record.getField(1));
	assertEquals("  ", record.getField(2));
    }
}

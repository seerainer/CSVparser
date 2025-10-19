package io.github.seerainer.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Demonstration tests for CSVParser features
 */
public class DemoTest {

    @SuppressWarnings("static-method")
    @Test
    public void testBasicParsing() throws CSVParseException {
	final var config = CSVConfiguration.builder().delimiter(',').quote('"').trimWhitespace(true)
		.initialBufferSize(512).maxFieldSize(1024 * 1024).build();

	final var options = CSVParsingOptions.builder().build();
	final var parser = new CSVParser(config, options);

	final var csvContent = """
		Name,Age,City,Description
		"John Doe",30,"New York","A software engineer with ""extensive"" experience"
		Jane Smith,25,Boston,Marketing specialist
		"Bob Johnson",35,"San Francisco","Senior developer, team lead"
		""";

	final var records = parser.parseByteArray(csvContent.getBytes());

	assertNotNull(records, "Records should not be null");
	assertEquals(4, records.size(), "Should have 4 records (header + 3 data rows)");

	final var firstRecord = records.get(1);
	assertEquals("John Doe", firstRecord.getField(0), "First field should be John Doe");
	assertEquals("30", firstRecord.getField(1), "Age should be 30");
	assertEquals("New York", firstRecord.getField(2), "City should be New York");
	assertTrue(firstRecord.getField(3).contains("extensive"), "Description should contain 'extensive'");
    }

    @SuppressWarnings("static-method")
    @Test
    public void testBOMHandling() throws CSVParseException {
	final var csvContent = "Product,Price\nLaptop,999.99\n";
	final var csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);

	// Add UTF-8 BOM manually
	final byte[] bom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
	final var csvWithBOM = new byte[bom.length + csvBytes.length];
	System.arraycopy(bom, 0, csvWithBOM, 0, bom.length);
	System.arraycopy(csvBytes, 0, csvWithBOM, bom.length, csvBytes.length);

	final var config = CSVConfiguration.builder().encoding(StandardCharsets.UTF_8).detectBOM(true).build();
	final var options = CSVParsingOptions.builder().build();
	final var parser = new CSVParser(config, options);

	final var records = parser.parseByteArray(csvWithBOM);

	assertNotNull(records, "Records should not be null");
	assertEquals(2, records.size(), "Should have 2 records");
	assertEquals("Product", records.get(0).getField(0), "First field should be Product (BOM stripped)");
	assertEquals("Laptop", records.get(1).getField(0), "Product name should be Laptop");
    }

    @SuppressWarnings("static-method")
    @Test
    public void testDifferentDelimiter() throws CSVParseException {
	final var semicolonConfig = CSVConfiguration.builder().delimiter(';').quote('"').build();
	final var options = CSVParsingOptions.builder().build();
	final var semicolonParser = new CSVParser(semicolonConfig, options);
	final var semicolonCsv = "Product;Price;Category\nLaptop;999.99;Electronics\nBook;19.99;Education";

	final var records = semicolonParser.parseByteArray(semicolonCsv.getBytes());

	assertNotNull(records, "Records should not be null");
	assertEquals(3, records.size(), "Should have 3 records");
	assertEquals("Laptop", records.get(1).getField(0), "First product should be Laptop");
	assertEquals("999.99", records.get(1).getField(1), "Laptop price should be 999.99");
    }

    @SuppressWarnings("static-method")
    @Test
    public void testEmptyValueHandling() throws CSVParseException {
	final var csvWithEmpties = """
		Name,Age,City,Email
		John,30,,john@example.com
		,25,Boston,
		Alice,,New York,alice@example.com
		,,,"
		Bob,35,,bob@example.com
		""";

	final var config = CSVConfiguration.builder().encoding(StandardCharsets.UTF_8).build();
	final var preserveOptions = CSVParsingOptions.builder().preserveEmptyFields(true)
		.treatConsecutiveDelimitersAsEmpty(true).skipEmptyLines(false).skipBlankLines(false).build();

	final var parser = new CSVParser(config, preserveOptions);
	final var records = parser.parseByteArray(csvWithEmpties.getBytes());

	assertNotNull(records, "Records should not be null");
	assertTrue(records.size() >= 5, "Should have at least 5 records");

	final var secondRecord = records.get(1);
	assertTrue(secondRecord.getEmptyFieldCount() >= 1, "Second record should have empty fields");
    }

    @SuppressWarnings("static-method")
    @Test
    public void testEncodingHandling() throws CSVParseException {
	final var csvContent = "Nom,Âge,Ville\n\"François\",25,\"Montréal\"\n";
	final var csvBytes = csvContent.getBytes(StandardCharsets.UTF_16LE);

	final var config = CSVConfiguration.builder().encoding(StandardCharsets.UTF_16LE).delimiter(',').quote('"')
		.build();
	final var options = CSVParsingOptions.builder().build();
	final var parser = new CSVParser(config, options);

	final var records = parser.parseByteArray(csvBytes);

	assertNotNull(records, "Records should not be null");
	assertEquals(2, records.size(), "Should have 2 records");
	assertEquals("François", records.get(1).getField(0), "Should correctly decode UTF-16LE");
	assertEquals("Montréal", records.get(1).getField(2), "Should correctly decode accented characters");
    }

    @SuppressWarnings("static-method")
    @Test
    public void testErrorHandling() throws CSVParseException {
	final var config = CSVConfiguration.builder().delimiter(',').quote('"').trimWhitespace(true)
		.initialBufferSize(512).maxFieldSize(1024 * 1024).build();

	// Test with tolerant parsing - parser should handle malformed data gracefully
	final var tolerantOptions = CSVParsingOptions.builder().failOnMalformedRecord(false).strictQuoting(false)
		.build();
	final var parser = new CSVParser(config, tolerantOptions);

	final var malformedCsv = "Name,Age\nJohn,30\n\"Unclosed quote,25";

	// Should not throw exception with tolerant options
	final var records = parser.parseByteArray(malformedCsv.getBytes());
	assertNotNull(records, "Records should not be null");

	// At least the valid rows should be parsed
	assertTrue(records.size() >= 2, "Should parse at least the header and first valid row");
    }

    @SuppressWarnings("static-method")
    @Test
    public void testErrorTolerance() throws CSVParseException {
	final var malformedCsv = """
		Name,Age,City
		John,30,New York
		"Unclosed quote,25,Boston
		Jane,invalid"quote,Chicago
		Bob,35,Seattle
		""";

	final var config = CSVConfiguration.builder().build();
	final var tolerantOptions = CSVParsingOptions.builder().failOnMalformedRecord(false)
		.allowUnescapedQuotesInFields(true).strictQuoting(false).build();

	final var parser = new CSVParser(config, tolerantOptions);
	final var records = parser.parseByteArray(malformedCsv.getBytes());

	assertNotNull(records, "Records should not be null");
	assertTrue(records.size() > 0, "Should parse some records despite errors");
    }

    @SuppressWarnings("static-method")
    @Test
    public void testFieldInfo() throws CSVParseException {
	final var csvContent = """
		Name,Age,City
		"John Doe",30,
		Jane Smith,"25",Boston
		""";

	final var config = CSVConfiguration.builder().build();
	final var options = CSVParsingOptions.builder().preserveEmptyFields(true).build();
	final var parser = new CSVParser(config, options);

	final var records = parser.parseByteArray(csvContent.getBytes());

	assertNotNull(records, "Records should not be null");
	assertTrue(records.size() >= 2, "Should have at least 2 records");

	final var firstDataRow = records.get(1);
	final var nameField = firstDataRow.getFieldInfo(0);
	assertTrue(nameField.wasQuoted(), "Name field should be quoted");
	assertEquals("John Doe", nameField.getValue(), "Name should be John Doe");

	final var cityField = firstDataRow.getFieldInfo(2);
	assertTrue(cityField.isEmpty(), "City field should be empty");
    }

    @SuppressWarnings("static-method")
    @Test
    public void testFlexibleQuoting() throws CSVParseException {
	final var messyCsv = """
		Name,Description,Price
		Product A,"Contains "quotes" and commas, lots of them",29.99
		Product B,Simple description,19.99
		"Product C","Fully quoted",39.99
		Product D,Has "embedded quotes without escaping,49.99
		""";

	final var config = CSVConfiguration.builder().build();
	final var flexibleOptions = CSVParsingOptions.builder().strictQuoting(false).allowUnescapedQuotesInFields(true)
		.failOnMalformedRecord(false).build();

	final var parser = new CSVParser(config, flexibleOptions);
	final var records = parser.parseByteArray(messyCsv.getBytes());

	assertNotNull(records, "Records should not be null");
	assertTrue(records.size() >= 4, "Should parse at least 4 records");

	final var productB = records.get(2);
	assertEquals("Product B", productB.getField(0), "Should correctly parse Product B");
	assertEquals("Simple description", productB.getField(1), "Should parse simple description");
    }

    @SuppressWarnings("static-method")
    @Test
    public void testLineHandling() throws CSVParseException {
	final var csvWithVariousLines = """
		Name,Age
		John,30

		Jane,25

		Bob,35
		""";

	final var config = CSVConfiguration.builder().build();

	// Test: skip empty lines
	final var skipEmptyOptions = CSVParsingOptions.builder().skipEmptyLines(true).skipBlankLines(false).build();
	final var parser1 = new CSVParser(config, skipEmptyOptions);
	final var records1 = parser1.parseByteArray(csvWithVariousLines.getBytes());

	assertNotNull(records1, "Records should not be null");

	// Test: keep all lines
	final var keepAllOptions = CSVParsingOptions.builder().skipEmptyLines(false).skipBlankLines(false).build();
	final var parser2 = new CSVParser(config, keepAllOptions);
	final var records2 = parser2.parseByteArray(csvWithVariousLines.getBytes());

	assertNotNull(records2, "Records should not be null");
	assertTrue(records2.size() >= records1.size(), "Keeping all lines should have more or equal records");
    }

    @SuppressWarnings("static-method")
    @Test
    public void testNullValueHandling() throws CSVParseException {
	final var csvWithNulls = """
		Name,Age,City,Notes
		John,30,NULL,Has experience
		Jane,,Boston,NULL
		Bob,35,,"No notes"
		""";

	final var config = CSVConfiguration.builder().build();
	final var nullOptions = CSVParsingOptions.builder().nullValueRepresentation("NULL").convertEmptyToNull(true)
		.trackFieldPositions(true).build();

	final var parser = new CSVParser(config, nullOptions);
	final var records = parser.parseByteArray(csvWithNulls.getBytes());

	assertNotNull(records, "Records should not be null");
	assertEquals(4, records.size(), "Should have 4 records (header + 3 data rows)");

	final var firstDataRow = records.get(1);
	assertTrue(firstDataRow.getNullFieldCount() >= 1, "First data row should have null fields");

	final var secondDataRow = records.get(2);
	assertTrue(secondDataRow.getNullFieldCount() >= 1, "Second data row should have null fields");
    }
}

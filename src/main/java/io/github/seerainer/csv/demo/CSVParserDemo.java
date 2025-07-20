package io.github.seerainer.csv.demo;

import io.github.seerainer.csv.CSVConfiguration;
import io.github.seerainer.csv.CSVParseException;
import io.github.seerainer.csv.CSVParser;
import io.github.seerainer.csv.CSVRecord;

public class CSVParserDemo {

	public static void main(final String[] args) {
		demonstrateBasicParsing();
		ByteArrayDemo.startDemo();
		Benchmark.startBenchmark();
	}

	private static void demonstrateBasicParsing() {
		// Create configuration
		final var config = CSVConfiguration.builder().delimiter(',').quote('"').trimWhitespace(true)
				.initialBufferSize(512).maxFieldSize(1024 * 1024).build();

		final var parser = new CSVParser(config);

		// Example 1: Parse a string
		final var csvContent = """
				Name,Age,City,Description
				"John Doe",30,"New York","A software engineer with ""extensive"" experience"
				Jane Smith,25,Boston,Marketing specialist
				"Bob Johnson",35,"San Francisco","Senior developer, team lead"
				""";

		try {
			System.out.println("=== Parsing String ===");
			final var records = parser.parseString(csvContent);

			records.forEach((final CSVRecord record) -> {
				System.out.println("Line " + record.getLineNumber() + ":");
				for (var i = 0; i < record.getFieldCount(); i++) {
					System.out.println("  Field " + i + ": '" + record.getField(i) + "'");
				}
				System.out.println();
			});
		} catch (final CSVParseException e) {
			System.err.println("Parse error: " + e.getMessage());
		}

		// Example 2: Different delimiter
		final var semicolonConfig = CSVConfiguration.builder().delimiter(';').quote('"').build();

		final var semicolonParser = new CSVParser(semicolonConfig);
		final var semicolonCsv = "Product;Price;Category\nLaptop;999.99;Electronics\nBook;19.99;Education";

		try {
			System.out.println("=== Parsing Semicolon Delimited ===");
			final var records = semicolonParser.parseString(semicolonCsv);

			records.forEach(System.out::println);
		} catch (final CSVParseException e) {
			System.err.println("Parse error: " + e.getMessage());
		}

		// Example 3: Error handling
		final var malformedCsv = "Name,Age\nJohn,30\n\"Unclosed quote,25";

		try {
			System.out.println("=== Testing Error Handling ===");
			parser.parseString(malformedCsv);
		} catch (final CSVParseException e) {
			System.err.println("Expected parse error: " + e.getMessage());
		}
	}
}
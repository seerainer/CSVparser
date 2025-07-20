package io.github.seerainer.csv.demo;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import io.github.seerainer.csv.CSVConfiguration;
import io.github.seerainer.csv.CSVParseException;
import io.github.seerainer.csv.CSVParser;
import io.github.seerainer.csv.CSVRecord;

public class ByteArrayDemo {

	private static void demonstrateBasicParsing() {
		System.out.println("=== Basic Byte Array Parsing ===");

		final var csvContent = """
				Name,Age,City,Description
				"John Doe",30,"New York","A software engineer with ""extensive"" experience"
				Jane Smith,25,Boston,Marketing specialist
				"Bob Johnson",35,"San Francisco","Senior developer, team lead"
				""";

		final var csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);

		final var config = CSVConfiguration.builder().encoding(StandardCharsets.UTF_8).delimiter(',').quote('"')
				.build();

		final var parser = new CSVParser(config);

		try {
			final var records = parser.parseByteArray(csvBytes);

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
	}

	private static void demonstrateBOMHandling() {
		System.out.println("=== BOM Detection ===");

		final var csvContent = "Product,Price\nLaptop,999.99\n";
		final var csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);

		// Add UTF-8 BOM manually
		final byte[] bom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
		final var csvWithBOM = new byte[bom.length + csvBytes.length];
		System.arraycopy(bom, 0, csvWithBOM, 0, bom.length);
		System.arraycopy(csvBytes, 0, csvWithBOM, bom.length, csvBytes.length);

		final var config = CSVConfiguration.builder().encoding(StandardCharsets.UTF_8).detectBOM(true).build();

		final var parser = new CSVParser(config);

		try {
			final var records = parser.parseByteArray(csvWithBOM);
			System.out.println("Successfully parsed CSV with BOM:");

			records.forEach((final CSVRecord record) -> System.out.println("  " + Arrays.toString(record.getFields())));
		} catch (final CSVParseException e) {
			System.err.println("Parse error: " + e.getMessage());
		}
	}

	private static void demonstrateEncodingHandling() {
		System.out.println("=== Encoding Handling (UTF-16) ===");

		final var csvContent = "Nom,Âge,Ville\n\"François\",25,\"Montréal\"\n";
		final var csvBytes = csvContent.getBytes(StandardCharsets.UTF_16LE);

		final var config = CSVConfiguration.builder().encoding(StandardCharsets.UTF_16LE).delimiter(',').quote('"')
				.build();

		final var parser = new CSVParser(config);

		try {
			final var records = parser.parseByteArray(csvBytes);

			records.forEach(
					(final CSVRecord record) -> System.out.println("Record: " + Arrays.toString(record.getFields())));
		} catch (final CSVParseException e) {
			System.err.println("Parse error: " + e.getMessage());
		}
	}

	public static void startDemo() {
		demonstrateBasicParsing();
		demonstrateEncodingHandling();
		demonstrateBOMHandling();
	}
}
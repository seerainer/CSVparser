package io.github.seerainer.csv;

import java.io.StringReader;

public class IteratorDebugTest {
    public static void main(final String[] args) throws Exception {
	final var config = CSVConfiguration.builder().delimiter(',').quote('"').trimWhitespace(true).build();

	final var options = CSVParsingOptions.builder().skipEmptyLines(false).preserveEmptyFields(true).build();

	final var parser = new CSVParser(config, options);

	final var csvContent = "P,Q\n1,2\n3,4\n5,6\n";
	System.out.println("CSV Content:");
	System.out.println(csvContent);
	System.out.println("\nParsing with parseString:");
	final var records = parser.parseString(csvContent);
	System.out.println("Total records: " + records.size());
	for (var i = 0; i < records.size(); i++) {
	    System.out.println(new StringBuilder().append("Record ").append(i).append(": ")
		    .append(String.join(" | ", records.get(i).getFields())).toString());
	}

	System.out.println("\nParsing with iterator:");
	try (var reader = new StringReader(csvContent); var iterator = parser.iterateReader(reader)) {

	    var count = 0;
	    while (iterator.hasNext()) {
		final var record = iterator.next();
		System.out.println(new StringBuilder().append("Record ").append(count).append(": ")
			.append(String.join(" | ", record.getFields())).toString());
		count++;
	    }
	    System.out.println("Total from iterator: " + count);
	}
    }
}

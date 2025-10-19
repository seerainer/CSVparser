package io.github.seerainer.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

public class BOMParsingTest {

    @SuppressWarnings("static-method")
    @Test
    public void iterateFileBOMAware() throws Exception {
	final var csv = "name,age\nJosé,30\n";
	final byte[] bom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
	final var bytes = csv.getBytes(StandardCharsets.UTF_8);
	final var data = new byte[bom.length + bytes.length];
	System.arraycopy(bom, 0, data, 0, bom.length);
	System.arraycopy(bytes, 0, data, bom.length, bytes.length);

	final var tmp = Files.createTempFile("csv_iter_utf8_bom", ".csv");
	Files.write(tmp, data);
	tmp.toFile().deleteOnExit();

	final var parser = new CSVParser(CSVConfiguration.builder().build(), CSVParsingOptions.builder().build());
	try (final var it = parser.iterateFile(tmp.toFile())) {
	    assertTrue(it.hasNext());
	    final var header = it.next();
	    assertEquals("name", header.getField(0));
	    assertTrue(it.hasNext());
	    final var rec = it.next();
	    assertEquals("José", rec.getField(0));
	}
    }

    @SuppressWarnings("static-method")
    @Test
    public void parseUtf16LeBomFile() throws Exception {
	final var csv = "name,age\r\nJosé,30\r\n";
	final byte[] bom = { (byte) 0xFF, (byte) 0xFE };
	final var bytes = csv.getBytes(StandardCharsets.UTF_16LE);
	final var data = new byte[bom.length + bytes.length];
	System.arraycopy(bom, 0, data, 0, bom.length);
	System.arraycopy(bytes, 0, data, bom.length, bytes.length);

	final var tmp = Files.createTempFile("csv_utf16le_bom", ".csv");
	Files.write(tmp, data);
	tmp.toFile().deleteOnExit();

	final var parser = new CSVParser(CSVConfiguration.builder().build(), CSVParsingOptions.builder().build());
	final var records = parser.parseFile(tmp.toFile());

	assertEquals(2, records.size());
	assertEquals("José", records.get(1).getField(0));
    }

    @SuppressWarnings("static-method")
    @Test
    public void parseUtf8BomByteArray() throws Exception {
	final var csv = "name,age\nJosé,30\n";
	final byte[] bom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
	final var bytes = csv.getBytes(StandardCharsets.UTF_8);
	final var data = new byte[bom.length + bytes.length];
	System.arraycopy(bom, 0, data, 0, bom.length);
	System.arraycopy(bytes, 0, data, bom.length, bytes.length);

	final var parser = new CSVParser(CSVConfiguration.builder().build(), CSVParsingOptions.builder().build());
	final var records = parser.parseByteArray(data);

	assertEquals(2, records.size(), "Should parse header + one record");
	assertEquals("José", records.get(1).getField(0));
    }
}

package io.github.seerainer.csv;

import java.util.Arrays;

public class CSVRecord {
	private final String[] fields;
	private final int lineNumber;

	public CSVRecord(final String[] fields, final int lineNumber) {
		this.fields = Arrays.copyOf(fields, fields.length);
		this.lineNumber = lineNumber;
	}

	public String getField(final int index) {
		if (index < 0 || index >= fields.length) {
			throw new IndexOutOfBoundsException("Field index " + index + " out of bounds");
		}
		return fields[index];
	}

	public int getFieldCount() {
		return fields.length;
	}

	public String[] getFields() {
		return Arrays.copyOf(fields, fields.length);
	}

	public int getLineNumber() {
		return lineNumber;
	}

	@Override
	public String toString() {
		return "CSVRecord{fields=" + Arrays.toString(fields) + ", line=" + lineNumber + "}";
	}
}
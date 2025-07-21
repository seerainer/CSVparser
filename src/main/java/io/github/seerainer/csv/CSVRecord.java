package io.github.seerainer.csv;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Enhanced CSV record with detailed field information
 */
public class CSVRecord {
	private final CSVFieldInfo[] fields;
	private final int lineNumber;
	private final int recordLength;
	private final boolean hadErrors;
	private final String[] errors;

	public CSVRecord(final CSVFieldInfo[] fields, final int lineNumber, final int recordLength, final boolean hadErrors,
			final String[] errors) {
		this.fields = Arrays.copyOf(fields, fields.length);
		this.lineNumber = lineNumber;
		this.recordLength = recordLength;
		this.hadErrors = hadErrors;
		this.errors = errors != null ? Arrays.copyOf(errors, errors.length) : new String[0];
	}

	/**
	 * Get count of empty fields in this record
	 */
	public int getEmptyFieldCount() {
		return (int) Arrays.stream(fields).filter(CSVFieldInfo::isEmpty).count();
	}

	/**
	 * Get indices of empty fields
	 */
	public int[] getEmptyFieldIndices() {
		return IntStream.range(0, fields.length).filter(i -> fields[i].isEmpty()).toArray();
	}

	public String[] getErrors() {
		return Arrays.copyOf(errors, errors.length);
	}

	public String getField(final int index) {
		return getFieldInfo(index).getValue();
	}

	public int getFieldCount() {
		return fields.length;
	}

	public CSVFieldInfo getFieldInfo(final int index) {
		if (index < 0 || index >= fields.length) {
			throw new IndexOutOfBoundsException("Field index " + index + " out of bounds");
		}
		return fields[index];
	}

	public CSVFieldInfo[] getFieldInfos() {
		return Arrays.copyOf(fields, fields.length);
	}

	public String[] getFields() {
		return Arrays.stream(fields).map(CSVFieldInfo::getValue).toArray(String[]::new);
	}

	public int getLineNumber() {
		return lineNumber;
	}

	/**
	 * Get count of null fields in this record
	 */
	public int getNullFieldCount() {
		return (int) Arrays.stream(fields).filter(CSVFieldInfo::isNull).count();
	}

	public int getRecordLength() {
		return recordLength;
	}

	public boolean hadErrors() {
		return hadErrors;
	}

	@Override
	public String toString() {
		return "CSVRecord{fields=%d, line=%d, length=%d, errors=%s}".formatted(Integer.valueOf(fields.length),
				Integer.valueOf(lineNumber), Integer.valueOf(recordLength), Boolean.valueOf(hadErrors));
	}
}
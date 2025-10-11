package io.github.seerainer.csv;

/**
 * Contains information about a parsed CSV field including its value and
 * metadata
 */
public class CSVFieldInfo {
    private final String value;
    private final boolean wasQuoted;
    private final boolean isEmpty;
    private final boolean isNull;
    private final int startPosition;
    private final int endPosition;
    private final int columnIndex;

    public CSVFieldInfo(final String value, final boolean wasQuoted, final boolean isEmpty, final boolean isNull,
	    final int startPosition, final int endPosition, final int columnIndex) {
	this.value = value;
	this.wasQuoted = wasQuoted;
	this.isEmpty = isEmpty;
	this.isNull = isNull;
	this.startPosition = startPosition;
	this.endPosition = endPosition;
	this.columnIndex = columnIndex;
    }

    public int getColumnIndex() {
	return columnIndex;
    }

    public int getEndPosition() {
	return endPosition;
    }

    public int getStartPosition() {
	return startPosition;
    }

    public String getValue() {
	return value;
    }

    public boolean isEmpty() {
	return isEmpty;
    }

    public boolean isNull() {
	return isNull;
    }

    @Override
    public String toString() {
	return "CSVField{value='%s', quoted=%s, empty=%s, null=%s, pos=%d-%d, col=%d}".formatted(value,
		Boolean.valueOf(wasQuoted), Boolean.valueOf(isEmpty), Boolean.valueOf(isNull),
		Integer.valueOf(startPosition), Integer.valueOf(endPosition), Integer.valueOf(columnIndex));
    }

    public boolean wasQuoted() {
	return wasQuoted;
    }
}
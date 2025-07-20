package io.github.seerainer.csv;

public class CSVParseException extends Exception {
	private static final long serialVersionUID = 1L;
	private final int lineNumber;
	private final int position;

	public CSVParseException(final String message, final int lineNumber, final int position) {
		super("Line %d, Position %d: %s".formatted(Integer.valueOf(lineNumber), Integer.valueOf(position), message));
		this.lineNumber = lineNumber;
		this.position = position;
	}

	public CSVParseException(final String message, final int lineNumber, final int position, final Throwable cause) {
		super("Line %d, Position %d: %s".formatted(Integer.valueOf(lineNumber), Integer.valueOf(position), message),
				cause);
		this.lineNumber = lineNumber;
		this.position = position;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public int getPosition() {
		return position;
	}
}
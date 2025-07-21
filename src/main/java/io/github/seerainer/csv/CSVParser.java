package io.github.seerainer.csv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CSVParser {

	private static class FieldParseResult {
		final ParseState newState;
		final boolean wasQuoted;
		final int nextPosition;
		final boolean fieldComplete;
		final String error;

		FieldParseResult(final ParseState newState, final boolean wasQuoted, final int nextPosition,
				final boolean fieldComplete, final String error) {
			this.newState = newState;
			this.wasQuoted = wasQuoted;
			this.nextPosition = nextPosition;
			this.fieldComplete = fieldComplete;
			this.error = error;
		}
	}

	private enum ParseState {
		FIELD_START, IN_FIELD, IN_QUOTED_FIELD, QUOTE_IN_QUOTED_FIELD, FIELD_END, RECORD_END
	}

	private static class RecordParseResult {
		final CSVRecord record;
		final int nextPosition;
		final int nextLineNumber;
		final boolean hasErrors;
		final String[] errors;
		final boolean isEmpty;
		final boolean isBlank;

		RecordParseResult(final CSVRecord record, final int nextPosition, final int nextLineNumber,
				final boolean hasErrors, final String[] errors, final boolean isEmpty, final boolean isBlank) {
			this.record = record;
			this.nextPosition = nextPosition;
			this.nextLineNumber = nextLineNumber;
			this.hasErrors = hasErrors;
			this.errors = errors;
			this.isEmpty = isEmpty;
			this.isBlank = isBlank;
		}
	}

	private final CSVConfiguration config;
	private final CSVParsingOptions options;
	private char[] charBuffer;
	private int charBufferPosition;

	public CSVParser(final CSVConfiguration config, final CSVParsingOptions options) {
		this.config = config;
		this.options = options;
		this.charBuffer = new char[config.getInitialBufferSize()];
		this.charBufferPosition = 0;
	}

	private void addCurrentField(final List<CSVFieldInfo> fields, final boolean wasQuoted, final int startPos,
			final int endPos, final int columnIndex) {
		var fieldValue = new String(charBuffer, 0, charBufferPosition);

		if (config.isTrimWhitespace() && !wasQuoted) {
			fieldValue = fieldValue.trim();
		}

		final var isEmpty = fieldValue.isEmpty();
		var isNull = false;

		// Handle null value representation
		if ((options.getNullValueRepresentation() != null && options.getNullValueRepresentation().equals(fieldValue))
				|| (options.isConvertEmptyToNull() && isEmpty)) {
			isNull = true;
			fieldValue = null;
		}

		final var fieldInfo = new CSVFieldInfo(fieldValue, wasQuoted, isEmpty, isNull, startPos, endPos, columnIndex);

		fields.add(fieldInfo);
		resetCharBuffer();
	}

	private void appendToCharBuffer(final char ch) {
		if (charBufferPosition >= charBuffer.length) {
			expandCharBuffer();
		}
		charBuffer[charBufferPosition++] = ch;
	}

	private char[] convertBytesToChars(final byte[] data, final int offset, final int length) {
		return new String(data, offset, length, config.getEncoding()).toCharArray();
	}

	private void expandCharBuffer() {
		final var newSize = Math.min(charBuffer.length * 2, config.getMaxFieldSize());
		final var newBuffer = new char[newSize];
		System.arraycopy(charBuffer, 0, newBuffer, 0, charBufferPosition);
		charBuffer = newBuffer;
	}

	private boolean isLineEnding(final char ch) {
		if (options.getCustomLineEndings() == null) {
			return ch == '\n' || ch == '\r';
		}
		for (final char ending : options.getCustomLineEndings()) {
			if (ch == ending) {
				return true;
			}
		}
		return false;
	}

	public List<CSVRecord> parseByteArray(final byte[] data) throws CSVParseException {
		return parseByteArray(data, 0, data.length);
	}

	private List<CSVRecord> parseByteArray(final byte[] data, final int offset, final int length)
			throws CSVParseException {
		var dataOffset = offset;
		var dataLength = length;

		if (config.isDetectBOM()) {
			final var bomInfo = BOMDetector
					.detectBOM(Arrays.copyOfRange(data, offset, Math.min(offset + 4, data.length)));

			if (bomInfo.getBomLength() > 0) {
				dataOffset += bomInfo.getBomLength();
				dataLength -= bomInfo.getBomLength();
			}
		}

		final var chars = convertBytesToChars(data, dataOffset, dataLength);

		return parseCharArrayWithOptions(chars);
	}

	private List<CSVRecord> parseCharArrayWithOptions(final char[] chars) throws CSVParseException {
		final List<CSVRecord> records = new ArrayList<>();

		var lineNumber = 1;
		var position = 0;
		while (position < chars.length) {
			final var result = parseRecord(chars, position, lineNumber);

			// Handle different line types based on options
			if (shouldSkipRecord(result)) {
				position = result.nextPosition;
				lineNumber = result.nextLineNumber;
				continue;
			}

			// Add record if it meets criteria
			if (result.record != null) {
				records.add(result.record);
			} else if (options.isFailOnMalformedRecord() && result.hasErrors) {
				throw new CSVParseException("Malformed record: " + String.join(", ", result.errors), lineNumber,
						position);
			}

			position = result.nextPosition;
			lineNumber = result.nextLineNumber;
		}

		return records;
	}

	private FieldParseResult parseFieldCharacter(final char currentChar, final int position, final ParseState state,
			final boolean wasQuoted, final int lineNumber) throws CSVParseException {
		switch (state) {
		case FIELD_START:
			if (currentChar == config.getQuote()) {
				return new FieldParseResult(ParseState.IN_QUOTED_FIELD, true, position + 1, false, null);
			} else if (currentChar == config.getDelimiter()) {
				if (options.isTreatConsecutiveDelimitersAsEmpty()) {
					return new FieldParseResult(ParseState.FIELD_START, false, position + 1, true, null);
				}
				return new FieldParseResult(ParseState.FIELD_START, false, position + 1, false, null);
			} else if (Character.isWhitespace(currentChar) && config.isTrimWhitespace()) {
				return new FieldParseResult(ParseState.FIELD_START, false, position + 1, false, null);
			} else {
				appendToCharBuffer(currentChar);
				return new FieldParseResult(ParseState.IN_FIELD, false, position + 1, false, null);
			}
		case IN_FIELD:
			if (currentChar == config.getDelimiter()) {
				return new FieldParseResult(ParseState.FIELD_START, wasQuoted, position + 1, true, null);
			} else if (currentChar == config.getQuote()) {
				if (options.isAllowUnescapedQuotesInFields()) {
					appendToCharBuffer(currentChar);
					return new FieldParseResult(ParseState.IN_FIELD, wasQuoted, position + 1, false, null);
				}
				final var error = "Unexpected quote in unquoted field at position " + position;
				return new FieldParseResult(state, wasQuoted, position + 1, false, error);
			} else {
				appendToCharBuffer(currentChar);
				return new FieldParseResult(ParseState.IN_FIELD, wasQuoted, position + 1, false, null);
			}
		case IN_QUOTED_FIELD:
			if (currentChar == config.getQuote()) {
				return new FieldParseResult(ParseState.QUOTE_IN_QUOTED_FIELD, wasQuoted, position + 1, false, null);
			}
			appendToCharBuffer(currentChar);
			return new FieldParseResult(ParseState.IN_QUOTED_FIELD, wasQuoted, position + 1, false, null);
		case QUOTE_IN_QUOTED_FIELD:
			if (currentChar == config.getQuote() && config.getEscape() == config.getQuote()) {
				// Escaped quote
				appendToCharBuffer(currentChar);
				return new FieldParseResult(ParseState.IN_QUOTED_FIELD, wasQuoted, position + 1, false, null);
			} else if (currentChar == config.getDelimiter()) {
				return new FieldParseResult(ParseState.FIELD_START, wasQuoted, position + 1, true, null);
			} else if (Character.isWhitespace(currentChar) && config.isTrimWhitespace()) {
				return new FieldParseResult(ParseState.FIELD_END, wasQuoted, position + 1, false, null);
			} else if (options.isStrictQuoting()) {
				final var error = "Invalid character after closing quote at position " + position;
				return new FieldParseResult(state, wasQuoted, position + 1, false, error);
			} else {
				// Allow characters after quotes in non-strict mode
				appendToCharBuffer(config.getQuote()); // Add the closing quote
				appendToCharBuffer(currentChar);
				return new FieldParseResult(ParseState.IN_FIELD, wasQuoted, position + 1, false, null);
			}
		case FIELD_END:
			if (currentChar == config.getDelimiter()) {
				return new FieldParseResult(ParseState.FIELD_START, wasQuoted, position + 1, true, null);
			} else if (Character.isWhitespace(currentChar) && config.isTrimWhitespace()) {
				return new FieldParseResult(ParseState.FIELD_END, wasQuoted, position + 1, false, null);
			} else {
				final var error = "Invalid character after quoted field at position " + position;
				return new FieldParseResult(state, wasQuoted, position + 1, false, error);
			}
		default:
			throw new CSVParseException("Invalid parser state", lineNumber, position);
		}
	}

	private RecordParseResult parseRecord(final char[] chars, final int startPosition, final int lineNumber)
			throws CSVParseException {
		final List<CSVFieldInfo> fields = new ArrayList<>();
		final List<String> errors = new ArrayList<>();

		resetCharBuffer();
		var state = ParseState.FIELD_START;
		var position = startPosition;
		var fieldStartPos = startPosition;
		var columnIndex = 0;
		var wasQuoted = false;
		var recordIsEmpty = true;
		var recordIsBlank = true;

		while (position < chars.length) {
			final var currentChar = chars[position];

			// Check for line endings
			if (isLineEnding(currentChar)) {
				// Handle end of record
				if (state == ParseState.IN_QUOTED_FIELD) {
					// Multi-line field - continue parsing
					appendToCharBuffer(currentChar);
					position++;
					if (currentChar == '\r' && position < chars.length && chars[position] == '\n') {
						appendToCharBuffer(chars[position]);
						position++;
					}
					continue;
				}
				// End of record
				if (state != ParseState.FIELD_START || charBufferPosition > 0 || options.isPreserveEmptyFields()) {
					addCurrentField(fields, wasQuoted, fieldStartPos, position, columnIndex);
				}

				// Skip line ending characters
				position++;
				if (currentChar == '\r' && position < chars.length && chars[position] == '\n') {
					position++;
				}

				break;
			}

			// Check if record is actually empty or blank
			if (!Character.isWhitespace(currentChar)) {
				recordIsEmpty = false;
				recordIsBlank = false;
			} else if (currentChar != ' ' && currentChar != '\t') {
				recordIsEmpty = false;
			}

			try {
				final var fieldResult = parseFieldCharacter(currentChar, position, state, wasQuoted, lineNumber);

				state = fieldResult.newState;
				wasQuoted = fieldResult.wasQuoted;
				position = fieldResult.nextPosition;

				if (fieldResult.fieldComplete) {
					addCurrentField(fields, wasQuoted, fieldStartPos, position, columnIndex);
					columnIndex++;
					fieldStartPos = position;
					wasQuoted = false;
					state = ParseState.FIELD_START;
				}

				if (fieldResult.error != null) {
					errors.add(fieldResult.error);
				}

			} catch (final CSVParseException e) {
				if (options.isFailOnMalformedRecord()) {
					throw e;
				}
				errors.add(e.getMessage());
				// Try to recover
				position++;
				state = ParseState.FIELD_START;
			}
		}

		// Create record
		CSVRecord record = null;
		if (!fields.isEmpty()) {
			record = new CSVRecord(fields.toArray(new CSVFieldInfo[0]), lineNumber, position - startPosition,
					!errors.isEmpty(), errors.toArray(new String[0]));
		}

		return new RecordParseResult(record, position, lineNumber + 1, !errors.isEmpty(), errors.toArray(new String[0]),
				recordIsEmpty, recordIsBlank);
	}

	private void resetCharBuffer() {
		charBufferPosition = 0;
	}

	private boolean shouldSkipRecord(final RecordParseResult result) {
		if ((result.record == null) || (options.isSkipEmptyLines() && result.isEmpty)) {
			return true;
		}

		if (options.isSkipBlankLines() && result.isBlank) {
			return true;
		}

		return false;
	}
}
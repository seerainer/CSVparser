package io.github.seerainer.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CSVParser {

	private enum ParseState {
		FIELD_START, IN_FIELD, IN_QUOTED_FIELD, QUOTE_IN_QUOTED_FIELD, FIELD_END
	}

	private final CSVConfiguration config;
	private char[] buffer;
	private int bufferPosition;
	private CharsetDecoder decoder;

	public CSVParser(final CSVConfiguration config) {
		this.config = config;
		this.buffer = new char[config.getInitialBufferSize()];
		this.bufferPosition = 0;
		initializeDecoder();
	}

	private void addCurrentField(final List<String> fields) {
		var field = new String(buffer, 0, bufferPosition);
		if (config.isTrimWhitespace()) {
			field = field.trim();
		}
		fields.add(field);
		resetBuffer();
	}

	private void appendToBuffer(final char ch, final int lineNumber, final int position) throws CSVParseException {
		if (bufferPosition >= config.getMaxFieldSize()) {
			throw new CSVParseException("Field size exceeds maximum allowed", lineNumber, position);
		}

		if (bufferPosition >= buffer.length) {
			expandBuffer();
		}

		buffer[bufferPosition++] = ch;
	}

	private char[] convertBytesToChars(final byte[] data, final int offset, final int length) throws CSVParseException {
		final var byteBuffer = ByteBuffer.wrap(data, offset, length);

		// Estimate output size (most characters are single byte in UTF-8)
		final var estimatedSize = Math.max(length, config.getInitialBufferSize());
		var charBuffer = CharBuffer.allocate(estimatedSize);

		var result = decoder.decode(byteBuffer, charBuffer, true);

		if (result.isError()) {
			throw new CSVParseException("Character encoding error: " + result.toString(), 0, 0);
		}

		// Handle buffer overflow by expanding
		while (result.isOverflow()) {
			final var newSize = charBuffer.capacity() * 2;
			final var newBuffer = CharBuffer.allocate(newSize);
			charBuffer.flip();
			newBuffer.put(charBuffer);
			charBuffer = newBuffer;

			result = decoder.decode(byteBuffer, charBuffer, true);
			if (result.isError()) {
				throw new CSVParseException("Character encoding error: " + result.toString(), 0, 0);
			}
		}

		// Flush the decoder
		decoder.flush(charBuffer);

		charBuffer.flip();
		final var chars = new char[charBuffer.remaining()];
		charBuffer.get(chars);

		// Reset decoder for next use
		decoder.reset();

		return chars;
	}

	private void expandBuffer() {
		final var newSize = Math.min(buffer.length * 2, config.getMaxFieldSize());
		final var newBuffer = new char[newSize];
		System.arraycopy(buffer, 0, newBuffer, 0, bufferPosition);
		buffer = newBuffer;
	}

	private ParseState handleFieldEnd(final char currentChar, final int lineNumber, final int position)
			throws CSVParseException {
		if (currentChar == config.getDelimiter()) {
			// Field already added in QUOTE_IN_QUOTED_FIELD state
			return ParseState.FIELD_START;
		}
		if (Character.isWhitespace(currentChar) && config.isTrimWhitespace()) {
			return ParseState.FIELD_END;
		}
		throw new CSVParseException("Invalid character after quoted field", lineNumber, position);
	}

	private ParseState handleFieldStart(final char currentChar, final int lineNumber, final int position)
			throws CSVParseException {
		if (currentChar == config.getQuote()) {
			return ParseState.IN_QUOTED_FIELD;
		}
		if (currentChar == config.getDelimiter()) {
			addCurrentField(new ArrayList<>());
			return ParseState.FIELD_START;
		}
		if (Character.isWhitespace(currentChar) && config.isTrimWhitespace()) {
			// Skip leading whitespace
			return ParseState.FIELD_START;
		}
		appendToBuffer(currentChar, lineNumber, position);
		return ParseState.IN_FIELD;
	}

	private ParseState handleInField(final char currentChar, final List<String> fields, final int lineNumber,
			final int position) throws CSVParseException {
		if (currentChar == config.getDelimiter()) {
			addCurrentField(fields);
			return ParseState.FIELD_START;
		}
		if (currentChar == config.getQuote()) {
			throw new CSVParseException("Unexpected quote in unquoted field", lineNumber, position);
		}
		appendToBuffer(currentChar, lineNumber, position);
		return ParseState.IN_FIELD;
	}

	private ParseState handleInQuotedField(final char currentChar, final int lineNumber, final int position)
			throws CSVParseException {
		if (currentChar == config.getQuote()) {
			return ParseState.QUOTE_IN_QUOTED_FIELD;
		}
		appendToBuffer(currentChar, lineNumber, position);
		return ParseState.IN_QUOTED_FIELD;
	}

	private ParseState handleQuoteInQuotedField(final char currentChar, final List<String> fields, final int lineNumber,
			final int position) throws CSVParseException {
		if (currentChar == config.getQuote() && config.getEscape() == config.getQuote()) {
			// Escaped quote (double quote)
			appendToBuffer(currentChar, lineNumber, position);
			return ParseState.IN_QUOTED_FIELD;
		}
		if (currentChar == config.getDelimiter()) {
			addCurrentField(fields);
			return ParseState.FIELD_START;
		}
		if (Character.isWhitespace(currentChar) && config.isTrimWhitespace()) {
			// Allow trailing whitespace after quoted field
			return ParseState.FIELD_END;
		}
		throw new CSVParseException("Invalid character after closing quote", lineNumber, position);
	}

	private void initializeDecoder() {
		this.decoder = config.getEncoding().newDecoder().onMalformedInput(CodingErrorAction.REPLACE)
				.onUnmappableCharacter(CodingErrorAction.REPLACE);
	}

	public List<CSVRecord> parseByteArray(final byte[] data) throws CSVParseException {
		return parseByteArray(data, 0, data.length);
	}

	private List<CSVRecord> parseByteArray(final byte[] data, final int offset, final int length)
			throws CSVParseException {

		// Handle BOM detection
		var dataOffset = offset;
		var dataLength = length;

		if (config.isDetectBOM()) {
			final var bomInfo = BOMDetector
					.detectBOM(Arrays.copyOfRange(data, offset, Math.min(offset + 4, data.length)));

			if (bomInfo.getBomLength() > 0) {
				dataOffset += bomInfo.getBomLength();
				dataLength -= bomInfo.getBomLength();

				// Update decoder if BOM indicates different encoding
				if (!bomInfo.getCharset().equals(config.getEncoding())) {
					this.decoder = bomInfo.getCharset().newDecoder().onMalformedInput(CodingErrorAction.REPLACE)
							.onUnmappableCharacter(CodingErrorAction.REPLACE);
				}
			}
		}

		// Convert bytes to chars efficiently
		final var chars = convertBytesToChars(data, dataOffset, dataLength);

		// Parse the character array
		return parseCharArray(chars);
	}

	private List<CSVRecord> parseCharArray(final char[] chars) throws CSVParseException {
		final List<CSVRecord> records = new ArrayList<>();
		final List<String> currentFields = new ArrayList<>();

		resetBuffer();
		var state = ParseState.FIELD_START;
		var lineNumber = 1;
		var position = 0;

		for (var i = 0; i < chars.length; i++) {
			final var currentChar = chars[i];
			position = i;

			// Handle line endings
			if (currentChar == '\n') {
				if (state == ParseState.IN_QUOTED_FIELD) {
					// Newline inside quoted field is part of the field
					appendToBuffer(currentChar, lineNumber, position);
					continue;
				}
				// End of record
				if (state != ParseState.FIELD_START || bufferPosition > 0) {
					addCurrentField(currentFields);
				}

				if (!currentFields.isEmpty()) {
					records.add(new CSVRecord(currentFields.toArray(new String[0]), lineNumber));
					currentFields.clear();
				}

				lineNumber++;
				state = ParseState.FIELD_START;
				resetBuffer();
				continue;
			}

			// Handle carriage return
			if (currentChar == '\r') {
				if (state != ParseState.IN_QUOTED_FIELD) {
					// Skip CR, let LF handle line ending
					continue;
				}
				// CR inside quoted field
				appendToBuffer(currentChar, lineNumber, position);
				continue;
			}

			// Parse character based on current state
			state = switch (state) {
			case FIELD_START -> handleFieldStart(currentChar, lineNumber, position);
			case IN_FIELD -> handleInField(currentChar, currentFields, lineNumber, position);
			case IN_QUOTED_FIELD -> handleInQuotedField(currentChar, lineNumber, position);
			case QUOTE_IN_QUOTED_FIELD -> handleQuoteInQuotedField(currentChar, currentFields, lineNumber, position);
			case FIELD_END -> handleFieldEnd(currentChar, lineNumber, position);
			};
		}

		// Handle end of input
		if (state == ParseState.IN_QUOTED_FIELD) {
			throw new CSVParseException("Unterminated quoted field", lineNumber, position);
		}

		if (state != ParseState.FIELD_START || bufferPosition > 0) {
			addCurrentField(currentFields);
		}

		if (!currentFields.isEmpty()) {
			records.add(new CSVRecord(currentFields.toArray(new String[0]), lineNumber));
		}

		return records;
	}

	public List<CSVRecord> parseFile(final Path filePath) throws IOException, CSVParseException {
		try (var reader = Files.newBufferedReader(filePath)) {
			return parseReader(reader);
		}
	}

	public List<CSVRecord> parseInputStream(final InputStream inputStream) throws IOException, CSVParseException {
		try (var reader = new BufferedReader(new InputStreamReader(inputStream))) {
			return parseReader(reader);
		}
	}

	public CSVRecord parseLine(final char[] line, final int lineNumber) throws CSVParseException {
		final List<String> fields = new ArrayList<>();

		resetBuffer();
		var state = ParseState.FIELD_START;
		var position = 0;

		for (var i = 0; i < line.length; i++) {
			final var currentChar = line[i];
			position = i;

			state = switch (state) {
			case FIELD_START -> handleFieldStart(currentChar, lineNumber, position);
			case IN_FIELD -> handleInField(currentChar, fields, lineNumber, position);
			case IN_QUOTED_FIELD -> handleInQuotedField(currentChar, lineNumber, position);
			case QUOTE_IN_QUOTED_FIELD -> handleQuoteInQuotedField(currentChar, fields, lineNumber, position);
			case FIELD_END -> handleFieldEnd(currentChar, lineNumber, position);
			};
		}

		// Handle end of line
		if (state == ParseState.IN_QUOTED_FIELD) {
			throw new CSVParseException("Unterminated quoted field", lineNumber, position);
		}

		if (state != ParseState.FIELD_START || bufferPosition > 0) {
			addCurrentField(fields);
		}

		return new CSVRecord(fields.toArray(new String[0]), lineNumber);
	}

	public List<CSVRecord> parseReader(final BufferedReader reader) throws IOException, CSVParseException {
		final List<CSVRecord> records = new ArrayList<>();
		String line;
		var lineNumber = 0;

		while ((line = reader.readLine()) != null) {
			lineNumber++;
			if (line.trim().isEmpty()) {
				continue; // Skip empty lines
			}

			final var record = parseLine(line.toCharArray(), lineNumber);
			records.add(record);
		}

		return records;
	}

	public List<CSVRecord> parseString(final String csvContent) throws CSVParseException {
		try (var stringReader = new StringReader(csvContent); var reader = new BufferedReader(stringReader)) {
			return parseReader(reader);
		} catch (final IOException e) {
			// Should not happen with StringReader
			throw new RuntimeException("Unexpected IOException", e);
		}
	}

	private void resetBuffer() {
		bufferPosition = 0;
	}
}
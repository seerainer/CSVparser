package io.github.seerainer.csv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class CSVParser {

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

    // Overload: charset-aware conversion
    private static char[] convertBytesToChars(final byte[] data, final int offset, final int length,
	    final Charset charset) {
	return new String(data, offset, length, charset).toCharArray();
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

    /**
     * Create a Reader that respects BOM detection and selects the appropriate
     * charset. Uses PushbackInputStream so bytes after the BOM are not lost.
     */
    private Reader createReaderHandlingBOM(final InputStream inputStream) throws IOException {
	if (!config.isDetectBOM()) {
	    return new InputStreamReader(inputStream, config.getEncoding());
	}

	final var pushback = new PushbackInputStream(inputStream, 4);
	final var firstBytes = new byte[4];
	final var bytesRead = pushback.read(firstBytes, 0, 4);
	if (bytesRead == -1) {
	    return new InputStreamReader(pushback, config.getEncoding());
	}

	final var bomInfo = BOMDetector.detectBOM(Arrays.copyOf(firstBytes, Math.min(bytesRead, 4)));
	final var charsetToUse = bomInfo.getBomLength() > 0 ? bomInfo.getCharset() : config.getEncoding();

	// Push back any bytes that are not part of the BOM so the reader sees them
	if (bytesRead > bomInfo.getBomLength()) {
	    pushback.unread(firstBytes, bomInfo.getBomLength(), bytesRead - bomInfo.getBomLength());
	}

	return new InputStreamReader(pushback, charsetToUse);
    }

    private void expandCharBuffer() {
	// Check if we've already reached the maximum field size
	if (charBuffer.length >= config.getMaxFieldSize()) {
	    throw new IllegalStateException(new StringBuilder().append("Field size exceeds maximum allowed size of ")
		    .append(config.getMaxFieldSize()).append(" characters").toString());
	}

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

    /**
     * Create an iterator for parsing CSV records from a file
     */
    public CSVRecordIterator iterateFile(final File file) throws IOException {
	try (final var inputStream = new FileInputStream(file);
		final var reader = createReaderHandlingBOM(inputStream)) {
	    return new CSVRecordIterator(reader, this);
	}
    }

    /**
     * Create an iterator for parsing CSV records from a file path
     */
    public CSVRecordIterator iterateFile(final Path path) throws IOException {
	try (final var inputStream = Files.newInputStream(path);
		final var reader = createReaderHandlingBOM(inputStream)) {
	    return new CSVRecordIterator(reader, this);
	}
    }

    /**
     * Create an iterator for parsing CSV records from a file path string
     */
    public CSVRecordIterator iterateFile(final String filePath) throws IOException {
	return iterateFile(new File(filePath));
    }

    /**
     * Create an iterator for parsing CSV records from a Reader
     */
    public CSVRecordIterator iterateReader(final Reader reader) {
	return new CSVRecordIterator(reader, this);
    }

    public List<CSVRecord> parseByteArray(final byte[] data) throws CSVParseException {
	return parseByteArray(data, 0, data.length);
    }

    private List<CSVRecord> parseByteArray(final byte[] data, final int offset, final int length)
	    throws CSVParseException {
	var dataOffset = offset;
	var dataLength = length;
	var charsetToUse = config.getEncoding();

	if (config.isDetectBOM()) {
	    final var bomInfo = BOMDetector
		    .detectBOM(Arrays.copyOfRange(data, offset, Math.min(offset + 4, data.length)));

	    if (bomInfo.getBomLength() > 0) {
		dataOffset += bomInfo.getBomLength();
		dataLength -= bomInfo.getBomLength();
		charsetToUse = bomInfo.getCharset();
	    }
	}

	final var chars = convertBytesToChars(data, dataOffset, dataLength, charsetToUse);

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

    /**
     * Parse CSV from a file
     */
    public List<CSVRecord> parseFile(final File file) throws IOException, CSVParseException {
	try (final var inputStream = new FileInputStream(file)) {
	    return parseInputStream(inputStream);
	}
    }

    /**
     * Parse CSV from a file path
     */
    public List<CSVRecord> parseFile(final Path path) throws IOException, CSVParseException {
	try (final var inputStream = Files.newInputStream(path)) {
	    return parseInputStream(inputStream);
	}
    }

    /**
     * Parse CSV from a file path string
     */
    public List<CSVRecord> parseFile(final String filePath) throws IOException, CSVParseException {
	return parseFile(new File(filePath));
    }

    /**
     * Parse CSV file using a callback for each record
     */
    public void parseFileWithCallback(final File file, final Consumer<CSVRecord> callback) throws IOException {
	try (var inputStream = new FileInputStream(file); var reader = createReaderHandlingBOM(inputStream)) {
	    parseWithCallback(reader, callback);
	}
    }

    /**
     * Parse CSV file using a callback for each record
     */
    public void parseFileWithCallback(final Path path, final Consumer<CSVRecord> callback) throws IOException {
	try (var inputStream = Files.newInputStream(path); var reader = createReaderHandlingBOM(inputStream)) {
	    parseWithCallback(reader, callback);
	}
    }

    /**
     * Parse CSV file using a callback for each record
     */
    public void parseFileWithCallback(final String filePath, final Consumer<CSVRecord> callback) throws IOException {
	parseFileWithCallback(new File(filePath), callback);
    }

    /**
     * Parse CSV from an InputStream
     */
    public List<CSVRecord> parseInputStream(final InputStream inputStream) throws IOException, CSVParseException {
	try (final var reader = createReaderHandlingBOM(inputStream)) {
	    return parseReader(reader);
	}
    }

    /**
     * Parse CSV from a Reader
     */
    public List<CSVRecord> parseReader(final Reader reader) throws IOException, CSVParseException {
	final var bufferedReader = reader instanceof final BufferedReader b ? b : new BufferedReader(reader);
	final var content = new StringBuilder();
	final var buffer = new char[8192];
	int charsRead;

	while ((charsRead = bufferedReader.read(buffer)) != -1) {
	    content.append(buffer, 0, charsRead);
	}

	return parseCharArrayWithOptions(content.toString().toCharArray());
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
	var lineEndingFound = false;

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

		lineEndingFound = true;
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

	// Handle EOF: add any remaining field data that wasn't terminated by a line
	// ending
	// Only add if we didn't already process the field during line-ending handling
	if (!lineEndingFound && (state != ParseState.FIELD_START || charBufferPosition > 0
		|| (options.isPreserveEmptyFields() && columnIndex > 0))) {
	    addCurrentField(fields, wasQuoted, fieldStartPos, position, columnIndex);
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

    /**
     * Parse CSV from a String
     */
    public List<CSVRecord> parseString(final String csvContent) throws CSVParseException {
	return parseCharArrayWithOptions(csvContent.toCharArray());
    }

    /**
     * Parse CSV using a callback for each record (memory-efficient for large files)
     */
    public void parseWithCallback(final Reader reader, final Consumer<CSVRecord> callback) throws IOException {
	try (var iterator = new CSVRecordIterator(reader, this)) {
	    while (iterator.hasNext()) {
		callback.accept(iterator.next());
	    }
	}
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

    private enum ParseState {
	FIELD_START, IN_FIELD, IN_QUOTED_FIELD, QUOTE_IN_QUOTED_FIELD, FIELD_END, RECORD_END
    }

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
}

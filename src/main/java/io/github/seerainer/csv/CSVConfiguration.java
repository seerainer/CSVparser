package io.github.seerainer.csv;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class CSVConfiguration {
    private final boolean detectBOM;
    private final boolean trimWhitespace;
    private final char delimiter;
    private final char quote;
    private final char escape;
    private final int initialBufferSize;
    private final int maxFieldSize;
    private final Charset encoding;

    public CSVConfiguration(final Builder builder) {
	this.delimiter = builder.delimiter;
	this.detectBOM = builder.detectBOM;
	this.encoding = builder.encoding;
	this.escape = builder.escape;
	this.initialBufferSize = builder.initialBufferSize;
	this.maxFieldSize = builder.maxFieldSize;
	this.quote = builder.quote;
	this.trimWhitespace = builder.trimWhitespace;
    }

    public static Builder builder() {
	return new Builder();
    }

    public char getDelimiter() {
	return delimiter;
    }

    public Charset getEncoding() {
	return encoding;
    }

    public char getEscape() {
	return escape;
    }

    public int getInitialBufferSize() {
	return initialBufferSize;
    }

    public int getMaxFieldSize() {
	return maxFieldSize;
    }

    public char getQuote() {
	return quote;
    }

    public boolean isDetectBOM() {
	return detectBOM;
    }

    public boolean isTrimWhitespace() {
	return trimWhitespace;
    }

    public static class Builder {
	private boolean trimWhitespace = true;
	private boolean detectBOM = true;
	private char delimiter = ',';
	private char quote = '"';
	private char escape = '"';
	private int initialBufferSize = 1024;
	private int maxFieldSize = 1024 * 1024;

	private Charset encoding = StandardCharsets.UTF_8;

	public CSVConfiguration build() {
	    return new CSVConfiguration(this);
	}

	public Builder delimiter(final char c) {
	    this.delimiter = c;
	    return this;
	}

	public Builder detectBOM(final boolean detect) {
	    this.detectBOM = detect;
	    return this;
	}

	public Builder encoding(final Charset cs) {
	    this.encoding = cs;
	    return this;
	}

	public Builder escape(final char c) {
	    this.escape = c;
	    return this;
	}

	public Builder initialBufferSize(final int size) {
	    this.initialBufferSize = size;
	    return this;
	}

	public Builder maxFieldSize(final int size) {
	    this.maxFieldSize = size;
	    return this;
	}

	public Builder quote(final char c) {
	    this.quote = c;
	    return this;
	}

	public Builder trimWhitespace(final boolean trim) {
	    this.trimWhitespace = trim;
	    return this;
	}
    }
}
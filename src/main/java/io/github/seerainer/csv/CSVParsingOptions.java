package io.github.seerainer.csv;

public class CSVParsingOptions {
	public static class Builder {
		private boolean preserveEmptyFields = true; // Changed default to true
		private boolean treatConsecutiveDelimitersAsEmpty = true;
		private boolean skipEmptyLines = false; // Changed default to false
		private boolean skipBlankLines = false;
		private String nullValueRepresentation = null;
		private boolean convertEmptyToNull = false;
		private boolean strictQuoting = true;
		private boolean allowUnescapedQuotesInFields = false;
		private boolean normalizeLineEndings = true;
		private char[] customLineEndings = null;
		private int maxRecordLength = Integer.MAX_VALUE;
		private boolean failOnMalformedRecord = true;
		private boolean trackFieldPositions = false;

		/**
		 * Allow unescaped quotes within fields (less strict parsing)
		 */
		public Builder allowUnescapedQuotesInFields(final boolean allow) {
			this.allowUnescapedQuotesInFields = allow;
			return this;
		}

		public CSVParsingOptions build() {
			return new CSVParsingOptions(this);
		}

		/**
		 * Convert empty strings to null values
		 */
		public Builder convertEmptyToNull(final boolean convert) {
			this.convertEmptyToNull = convert;
			return this;
		}

		/**
		 * Custom line ending characters (default is \n and \r\n)
		 */
		public Builder customLineEndings(final char[] endings) {
			this.customLineEndings = endings;
			return this;
		}

		/**
		 * Whether to fail parsing on malformed records or skip them
		 */
		public Builder failOnMalformedRecord(final boolean fail) {
			this.failOnMalformedRecord = fail;
			return this;
		}

		/**
		 * Maximum allowed record length (number of characters)
		 */
		public Builder maxRecordLength(final int maxLength) {
			this.maxRecordLength = maxLength;
			return this;
		}

		/**
		 * Normalize different line ending types to \n
		 */
		public Builder normalizeLineEndings(final boolean normalize) {
			this.normalizeLineEndings = normalize;
			return this;
		}

		/**
		 * Specific string representation for null values (e.g., "NULL", "\\N")
		 */
		public Builder nullValueRepresentation(final String nullRep) {
			this.nullValueRepresentation = nullRep;
			return this;
		}

		/**
		 * Whether to preserve empty fields (e.g., "a,,c" has 3 fields, middle one
		 * empty)
		 */
		public Builder preserveEmptyFields(final boolean preserve) {
			this.preserveEmptyFields = preserve;
			return this;
		}

		/**
		 * Skip lines that contain only whitespace
		 */
		public Builder skipBlankLines(final boolean skip) {
			this.skipBlankLines = skip;
			return this;
		}

		/**
		 * Skip completely empty lines (no characters at all)
		 */
		public Builder skipEmptyLines(final boolean skip) {
			this.skipEmptyLines = skip;
			return this;
		}

		/**
		 * Enforce strict quoting rules
		 */
		public Builder strictQuoting(final boolean strict) {
			this.strictQuoting = strict;
			return this;
		}

		/**
		 * Track field positions (column, character position) for debugging
		 */
		public Builder trackFieldPositions(final boolean track) {
			this.trackFieldPositions = track;
			return this;
		}

		/**
		 * Whether consecutive delimiters create empty fields If false, "a,,c" becomes
		 * ["a", "c"] If true, "a,,c" becomes ["a", "", "c"]
		 */
		public Builder treatConsecutiveDelimitersAsEmpty(final boolean treat) {
			this.treatConsecutiveDelimitersAsEmpty = treat;
			return this;
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	private final boolean preserveEmptyFields;
	private final boolean treatConsecutiveDelimitersAsEmpty;
	private final boolean skipEmptyLines;
	private final boolean skipBlankLines;
	private final String nullValueRepresentation;
	private final boolean convertEmptyToNull;
	private final boolean strictQuoting;
	private final boolean allowUnescapedQuotesInFields;
	private final boolean normalizeLineEndings;
	private final char[] customLineEndings;
	private final int maxRecordLength;
	private final boolean failOnMalformedRecord;
	private final boolean trackFieldPositions;

	private CSVParsingOptions(final Builder builder) {
		this.preserveEmptyFields = builder.preserveEmptyFields;
		this.treatConsecutiveDelimitersAsEmpty = builder.treatConsecutiveDelimitersAsEmpty;
		this.skipEmptyLines = builder.skipEmptyLines;
		this.skipBlankLines = builder.skipBlankLines;
		this.nullValueRepresentation = builder.nullValueRepresentation;
		this.convertEmptyToNull = builder.convertEmptyToNull;
		this.strictQuoting = builder.strictQuoting;
		this.allowUnescapedQuotesInFields = builder.allowUnescapedQuotesInFields;
		this.normalizeLineEndings = builder.normalizeLineEndings;
		this.customLineEndings = builder.customLineEndings;
		this.maxRecordLength = builder.maxRecordLength;
		this.failOnMalformedRecord = builder.failOnMalformedRecord;
		this.trackFieldPositions = builder.trackFieldPositions;
	}

	public char[] getCustomLineEndings() {
		return customLineEndings;
	}

	public int getMaxRecordLength() {
		return maxRecordLength;
	}

	public String getNullValueRepresentation() {
		return nullValueRepresentation;
	}

	public boolean isAllowUnescapedQuotesInFields() {
		return allowUnescapedQuotesInFields;
	}

	public boolean isConvertEmptyToNull() {
		return convertEmptyToNull;
	}

	public boolean isFailOnMalformedRecord() {
		return failOnMalformedRecord;
	}

	public boolean isNormalizeLineEndings() {
		return normalizeLineEndings;
	}

	public boolean isPreserveEmptyFields() {
		return preserveEmptyFields;
	}

	public boolean isSkipBlankLines() {
		return skipBlankLines;
	}

	public boolean isSkipEmptyLines() {
		return skipEmptyLines;
	}

	public boolean isStrictQuoting() {
		return strictQuoting;
	}

	public boolean isTrackFieldPositions() {
		return trackFieldPositions;
	}

	public boolean isTreatConsecutiveDelimitersAsEmpty() {
		return treatConsecutiveDelimitersAsEmpty;
	}
}
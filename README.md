# CSVparser

A fast, flexible, and modern CSV parser for Java. Supports custom delimiters, quoting, BOM detection, and encoding handling. Designed for performance and ease of use.

## Features

- **Custom Delimiters & Quotes:** Easily configure delimiter and quote characters.
- **Whitespace Trimming:** Optionally trim whitespace from fields.
- **Encoding Support:** Parse CSV data in UTF-8, UTF-16, and more.
- **BOM Detection:** Automatically detects and handles Byte Order Marks.
- **Error Handling:** Detailed exceptions with line and position info.
- **Efficient Parsing:** Optimized for large files and high throughput.
- **Flexible Input:** Parse from byte arrays (with encoding), and easily extend to files or streams.
- **Detailed Field Info:** Each field includes metadata (quoted, empty, null, position, etc).
- **Configurable Parsing Options:** Control error tolerance, empty/null handling, line skipping, and more.

## Quick Start

Add the source files to your Java project. See `Demo.java` for usage examples.

```java
import io.github.seerainer.csv.*;

public class Example {
    public static void main(String[] args) throws CSVParseException {
        var config = CSVConfiguration.builder()
            .delimiter(',')
            .quote('"')
            .trimWhitespace(true)
            .build();

        var options = CSVParsingOptions.builder()
            .allowUnescapedQuotesInFields(true)
            .failOnMalformedRecord(false)
            .build();

        var parser = new CSVParser(config, options);
        var csv = "Name,Age\n\"Alice\",30\nBob,25";
        var records = parser.parseByteArray(csv.getBytes());

        for (CSVRecord record : records) {
            System.out.println(record);
        }
    }
}
```

## Configuration

Customize parsing via the builder:

- `delimiter(char)` — Field separator (default: `,`)
- `quote(char)` — Quote character (default: `"`)
- `escape(char)` — Escape character (default: `"`)
- `trimWhitespace(boolean)` — Trim fields (default: `true`)
- `encoding(Charset)` — Character encoding (default: UTF-8)
- `detectBOM(boolean)` — Enable BOM detection (default: `true`)
- `initialBufferSize(int)` — Buffer size for fields
- `maxFieldSize(int)` — Maximum allowed field size

## Parsing Methods

- `parseByteArray(byte[] data)` — Parse CSV from a byte array (with encoding and BOM support)

## Parsing Options

Configure parsing behavior with `CSVParsingOptions.builder()`:

- `preserveEmptyFields(boolean)` — Keep empty fields (default: `true`)
- `treatConsecutiveDelimitersAsEmpty(boolean)` — Treat consecutive delimiters as empty fields (default: `true`)
- `skipEmptyLines(boolean)` — Skip empty lines (default: `false`)
- `skipBlankLines(boolean)` — Skip blank (whitespace-only) lines (default: `false`)
- `nullValueRepresentation(String)` — String to treat as null (e.g., `"NULL"`)
- `convertEmptyToNull(boolean)` — Convert empty fields to null
- `strictQuoting(boolean)` — Enforce strict quoting rules (default: `true`)
- `allowUnescapedQuotesInFields(boolean)` — Allow unescaped quotes in fields
- `failOnMalformedRecord(boolean)` — Throw on malformed records (default: `true`)
- `trackFieldPositions(boolean)` — Track field positions for debugging

## Error Handling

Parsing errors throw `CSVParseException` with line and position details. If `failOnMalformedRecord(false)` is set, errors are attached to each `CSVRecord`.

## Demo

Run the main demo:

```shell
java io.github.seerainer.csv.demo.Demo
```

See `Demo.java` for comprehensive usage examples, including BOM handling, encodings, error tolerance, and more.

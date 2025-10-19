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

## Gradle

CSVparser dependency can be added via the jitpack repository.

```gradle
   repositories {
        mavenCentral()
        maven { url "https://jitpack.io" }
   }
   dependencies {
         implementation 'com.github.seerainer:CSVparser:0.2.1'
   }
```

## Quick Start

Quick example:

```java
import io.github.seerainer.csv.*;

public class Example {
    public static void main(String[] args) throws CSVParseException {
        var config = CSVConfiguration.builder()
            .delimiter(',')
            .quote('"')
            .trimWhitespace(true)
            .build();

        var options = CSVParsingOptions.builder().build();
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

This library provides multiple parsing approaches — choose the one that best fits your memory and control requirements.

### File-based parsing (load all records into memory)

- Methods: `parseFile(File)`, `parseFile(Path)`, `parseFile(String)`, `parseInputStream(InputStream)`, `parseReader(Reader)`
- Use when the file is reasonably small and you want all records in a List.

Quick example:

```java
CSVParser parser = new CSVParser(config, options);
// From path
List<CSVRecord> records = parser.parseFile("data.csv");
// From Reader
try (Reader r = new FileReader("data.csv")) {
    List<CSVRecord> r2 = parser.parseReader(r);
}
```

### Iterator-based parsing (manual control, streaming)

- Methods: `iterateReader(Reader)`, `iterateFile(File)`, `iterateFile(Path)`, `iterateFile(String)`
- Returns `CSVRecordIterator` which implements `Iterator<CSVRecord>` and `Closeable`.
- Use when you want to process records one-by-one and possibly stop early without reading the whole file.

Quick example (try-with-resources):

```java
try (CSVRecordIterator it = parser.iterateFile("data.csv")) {
    while (it.hasNext()) {
        CSVRecord rec = it.next();
        // process record
        if (someCondition(rec)) break; // can stop early
    }
}
```

### Callback-based parsing (maximum memory efficiency)

- Methods: `parseWithCallback(Reader, Consumer<CSVRecord>)`, `parseFileWithCallback(File/Path/String, Consumer<CSVRecord>)`
- Use when processing very large files where you don't want to retain records in memory — records are delivered to your callback as they're parsed.

Quick example:

```java
parser.parseFileWithCallback("large.csv", record -> {
    // handle record, e.g. write to DB
    db.insert(record.getFields());
});

// From Reader
try (Reader r = new FileReader("data.csv")) {
    parser.parseWithCallback(r, rec -> System.out.println(rec));
}
```

### Byte-array and String parsing (in-memory sources)

- Methods: `parseByteArray(byte[] data)`, `parseString(String csvContent)`
- Use when the CSV content is already in memory (small to medium size).

Quick example:

```java
byte[] bytes = Files.readAllBytes(Path.of("data.csv"));
List<CSVRecord> records = parser.parseByteArray(bytes);

List<CSVRecord> fromString = parser.parseString("A,B\n1,2\n");
```

Notes
- All parsing methods may throw `IOException` (I/O errors) and `CSVParseException` (malformed CSV). Handle them as appropriate.
- When using iterator or reader-based APIs, always close the returned `CSVRecordIterator` (it implements `Closeable`) — prefer try-with-resources to avoid leaking file handles.

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

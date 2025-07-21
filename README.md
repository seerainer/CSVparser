# CSVparser

A fast, flexible, and modern CSV parser for Java. Supports custom delimiters, quoting, BOM detection, and encoding handling. Designed for performance and ease of use.

## Features

- **Custom Delimiters & Quotes:** Easily configure delimiter and quote characters.
- **Whitespace Trimming:** Optionally trim whitespace from fields.
- **Encoding Support:** Parse CSV data in UTF-8, UTF-16, and more.
- **BOM Detection:** Automatically detects and handles Byte Order Marks.
- **Error Handling:** Detailed exceptions with line and position info.
- **Efficient Parsing:** Optimized for large files and high throughput.
- **Flexible Input:** Parse from strings, byte arrays, files, or streams.

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

        var parser = new CSVParser(config);
        var csv = "Name,Age\n\"Alice\",30\nBob,25";
        var records = parser.parseString(csv);

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

- `parseString(String csvContent)`
- `parseByteArray(byte[] data)`
- `parseFile(Path filePath)`
- `parseInputStream(InputStream inputStream)`

## Error Handling

Parsing errors throw `CSVParseException` with line and position details.

## Demo

Run the main demo:

```shell
java io.github.seerainer.csv.demo.Demo
```


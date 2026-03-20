# CSVParser Repository Guide

This document provides essential instructions for AI agents and developers working on the CSVParser repository.

## 1. Build and Test Commands

This project uses Gradle. Always use the provided wrapper (`gradlew`).

### Basic Commands
- **Build Project**:
  ```bash
  ./gradlew build
  ```
- **Clean Build**:
  ```bash
  ./gradlew clean build
  ```
- **Compile Java**:
  ```bash
  ./gradlew compileJava
  ```

### Testing
- **Run All Tests**:
  ```bash
  ./gradlew test
  ```
- **Run a Specific Test Class**:
  ```bash
  ./gradlew test --tests "io.github.seerainer.csv.DemoTest"
  ```
- **Run a Specific Test Method**:
  ```bash
  ./gradlew test --tests "io.github.seerainer.csv.DemoTest.testBasicParsing"
  ```
- **Run with Info/Debug Logging** (useful for diagnosing failures):
  ```bash
  ./gradlew test -i
  ```

## 2. Code Style & Conventions

### Java Version
- **Target Version**: Java 25.
- Utilize modern Java features appropriately (e.g., text blocks `"""`, `var`, strict null checks).

### Formatting and Syntax
- **Indentation**: 4 spaces.
- **Braces**: Always use braces `{}` for control structures (`if`, `else`, `for`, `do`, `while`), even for single statements.
- **Final**: aggressively use the `final` keyword.
  - **Parameters**: Method arguments should be `final`.
  - **Local Variables**: Local variables should be `final` unless mutation is strictly required.
  - **Fields**: Make fields `final` whenever possible.
- **Var**: Use `var` for local variable type inference where the type is obvious from the right-hand side.
  ```java
  // Good
  final var list = new ArrayList<String>();
  final var parser = new CSVParser(config, options);
  
  // Avoid if type is unclear
  final var result = complexMethod(); // Better: final ResultType result = ...
  ```

### Naming
- **Classes/Interfaces**: PascalCase (e.g., `CSVParser`, `BOMDetector`).
- **Methods/Variables**: camelCase (e.g., `parseByteArray`, `charBufferPosition`).
- **Constants**: UPPER_SNAKE_CASE (e.g., `DEFAULT_BUFFER_SIZE`).
- **Packages**: lowercase (e.g., `io.github.seerainer.csv`).

### Imports
- **Explicit Imports**: Do not use wildcard imports (`import java.util.*`). Import each class explicitly.
- **Order**: Standard IDE ordering (java/javax first, then third-party, then internal).

### Error Handling
- Use the custom `CSVParseException` for parsing errors.
- Include line number and character position in parse exceptions.
- Gracefully handle BOM detection and different line endings.

### Testing Guidelines
- **Framework**: JUnit 5 (Jupiter).
- **Style**:
  - Use `@Test` annotation.
  - Use `assertNotNull`, `assertEquals`, `assertTrue` from `org.junit.jupiter.api.Assertions`.
  - Use `@SuppressWarnings("static-method")` for test methods that don't access instance state.
  - Use text blocks for multiline CSV content in tests.

## 3. Project Structure
- **Source Code**: `src/main/java/io/github/seerainer/csv`
- **Tests**: `src/test/java/io/github/seerainer/csv`
- **Build Configuration**: `build.gradle` (Groovy DSL)

## 4. Key Classes
- `CSVParser`: Main entry point for parsing. Handles buffer management and state machine.
- `CSVConfiguration`: Immutable configuration for the parser (delimiter, quote char, etc.).
- `CSVParsingOptions`: Runtime options (strictness, empty field handling).
- `CSVRecord`: Represents a parsed row.
- `BOMDetector`: Utility for handling Byte Order Marks.

## 5. Development Workflow
1. **Analyze**: Read relevant files using `ls` and `read` tools.
2. **Test-Driven**: Check for existing tests in `src/test/java`. Create new tests for new features/fixes *before* or *during* implementation.
3. **Verify**: Always run `./gradlew test` after changes.
4. **Style Check**: Ensure code follows the `final var` convention strictly.

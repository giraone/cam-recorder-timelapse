# Guidelines for Agents

## Implementation Requirements

- Must run on *Linux*, *Windows*, *macOS*, *Android* and *iOS*.
- Build system is *Maven*.
  - Use installed `mvn`, no Maven wrapper (`mvnw`).
  - Use the latest versions of dependencies and plugins, that are not release candidates or milestones.
  - Use semantic versioning for releases of this project.
  - Use a property for all dependency versions and put it into the properties section of `pom.xml` using a name ending in `.version`.
- Programming language is *Java*.
  - Source and target JVM is *JDK 21*.
  - File encoding is *UTF-8*.
  - Use installed `java` and `javac` located in directory defined by environment variable `$JAVA_HOME`.
  - Use US English for *JavaDoc*.
- Dependencies for source code
  - Preferred library for PDF operations is *PDFBox*.
  - Logging framework is *SLF4J*.
    - Use static Logger from LoggerFactory, e.g. `private static final Logger LOG = LoggerFactory.getLogger(NameOfClass.class);`
- Dependencies for test code
  - Unit test framework is *JUnit5*.
  - Used assertion language in tests is *AssertJ*.
- Testing
  - Include unit tests and integration tests.
  - Ensure high code coverage.
- Code Changes
  - Document summary of code changes in `CHANGELOG.md`. Add latest change on top.

## General Code Formatting and Coding Conventions

- Follow Java guidelines for code formatting and coding conventions.
  - Use clear and descriptive names for classes, methods, and variables.
  - Avoid abbreviations unless they are widely recognized.
  - Use camelCase for method and variable names.
  - Use PascalCase for class and interface names.
- Respect indentation and spacing rules defined in [.editorconfig](.editorconfig).
- Write modular and reusable code.
- Include JavaDoc for all public classes and methods.
- Include JavaDoc for complex methods and classes, even if they are not public, explaining their purpose and usage.
- Include comments to explain complex logic within methods.
- Do not use *var* for variable declarations, use explicit types.
- Do not use *Lombok*.

## Convention for Writing Unit Tests

- Unit test framework is *JUnit5*.
- Used assertion language in tests is *AssertJ*.
- Do not use mocking frameworks in tests, use test doubles, if needed.
- Normal unit tests for a class "Clazz.java" are located in the same package as the class under test and named "ClazzTest.java".
  They are perform in the mvn test phase.
- Use AAA pattern for tests (Arrange, Act, Assert) and separate the sections of the test code
  by a three slash comment line containing arrange, act, assert, e.g. `/// arrange`.
- When testing with multiple test data sets, use JUnit5 parameterized tests (`@ParameterizedTest`).
- When testing for exceptions, use AssertJ's *assertThatThrownBy*.
- When using temporary files in tests, ensure they are properly deleted after the test execution.
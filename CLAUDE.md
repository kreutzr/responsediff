# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**ResponseDiff** is a Java-based regression testing library that compares HTTP responses between a reference installation and a candidate (software under test) installation. It supports functional testing (expected values, JSONPath validation) and non-functional testing (response time constraints). When comparison mode is disabled it can also drive successive HTTP requests as a lightweight Postman/Bruno alternative.

**Version:** 1.5.1-SNAPSHOT | **Target:** Java 17  
**Documentation:** `README.md` (overview), `doc/manual_en.adoc` / `doc/manual_de.adoc` (complete reference), `doc/release-notes.adoc`

## Build & Test Commands

```bash
# Build
mvn clean install

# Full build with docs, vulnerability scan, and SBOM
mvn clean install -P documentation -P dependency-check -P license-summary -P maven-central

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=com.github.kreutzr.responsediff.tools.ComparatorHelperTest

# Run a single test method
mvn test -Dtest=com.github.kreutzr.responsediff.tools.ComparatorHelperTest#testThatEqualsWorksForLocalDate


# OWASP vulnerability scan
mvn verify -P dependency-check
```

## Architecture

### Data Flow

```
CLI entry point
    │
    ▼
[1] Scenario parsing       XML test scenarios → internal data structures
    │
    ▼
[2] Test execution         Send HTTP requests, receive responses
    │
    ▼
[3] Actual/expected diff   JSONPath evaluation on response bodies
    │
    ▼
[4] Result aggregation     Append test results to XML structure
    │
    ▼
[5] XSLT transformation    Result XML → AsciiDoc (.adoc)
    │
    ▼
[6] AsciidoctorJ rendering .adoc → HTML and/or PDF
```

### Entry Points

ResponseDiff has three entry points:

1. **`ResponseDiff.main()`** — regression testing; reads a JSON config (`ResponseDiffConfiguration`), loads an XML test setup, runs `TestSetHandler`, and emits XML → AsciiDoc → PDF/HTML reports.
2. **`CompareJson.main()`** — standalone comparison of two JSON files.
3. **`CloneTestSetup.cloneTestSetup()`** — copies test XML and referenced files to a new location.

CLI invocation example (from `doc/start-responseDiff`):
```bash
java -cp ./responsediff-[1.5.1-SNAPSHOT].jar \
  -Dlog4j2.configurationFile=file:./log4j2.xml \
  com.github.kreutzr.responsediff.ResponseDiff \
  "$( eval echo '{ [config as JSON] }')"
```

### Key Classes

| Class | Role |
|---|---|
| `ResponseDiff` | Entry point; orchestrates setup, execution, reporting |
| `ResponseDiffConfiguration` | Maps the JSON config passed to `main()` |
| `TestSetHandler` | Depth-first traversal; per-test HTTP + filter + validate |
| `HttpHandler` | Prepares/sends HTTP requests via `java.net.http`; applies request & response filters |
| `ValidationHandler` | Computes diffs, strips white noise, validates expected values |
| `JsonDiff` | Core JSON comparison engine; returns `JsonDiffEntry` list |
| `JsonPathHelper` | Wrapper around `com.jayway.jsonpath` |
| `VariablesHandler` | `${var}` / `${service.var}` substitution in URLs, headers, bodies |
| `VariablesCalculator` | Evaluates variable expressions at runtime |
| `XmlFileHandler` | JAXB read/write of test setup XML with XSD validation |
| `XmlToJson` | XML-to-JSON conversion |
| `OuterContext` | Runtime state: service URLs, filter registry, timeout, epsilon |

### Key Configuration Parameters

Passed as JSON to `main()`, mapped to `ResponseDiffConfiguration`:

- `rootPath` — base path for resolving test scenario files
- `xmlFilePath` — path to the entry-point `setup.xml`
- `candidateServiceUrl` / `referenceServiceUrl` — service URLs under comparison
- `candidateHeaders` / `referenceHeaders` — auth and other request headers per service

### Filter Architecture (`filter` package)

Filters implement `DiffFilter` (parameter management) and either `DiffRequestFilter` or `DiffResponseFilter`. `FilterRegistryHelper` instantiates them via reflection from XML `<filterRegistry>` declarations.

Built-in filters:
- **Request:** `RemoveHeaderRequestFilter`, `SetVariablesRequestFilter` (JSONPath extraction → variables)
- **Response:** `XmlToJsonResponseFilter`, `TextToJsonResponseFilter`, `NormalizeJsonBodyResponseFilter`, `SortJsonBodyResponseFilter`

To add a custom filter, extend `DiffRequestFilterImpl` or `DiffResponseFilterImpl`, override `registerFilterParameterNames()` and `apply()`, then register it in the XML `<filterRegistry>`.

### White Noise

White noise is the set of differences observed between the *reference* and *control* installations (two instances of the same software). These differences are considered acceptable environmental variance and are subtracted from the candidate-vs-reference diff, so only genuine regressions are reported.

### Variable Scoping

Variables support service-specific scoping:
- `${varName}` — applies to all services
- `${candidate.varName}` / `${reference.varName}` — scoped to a single service

`SetVariablesRequestFilter` can extract values from a response via JSONPath and inject them as variables for subsequent requests.

### XML Test Scenario Format

Test scenarios are XML files validated against `src/main/resources/com/github/kreutzr/responsediff/responseDiffSetup.xsd`. The XSD root element is `XmlResponseDiffSetup`. JAXB-generated classes live in `target/generated-sources/jaxb/`.

- Entry point per scenario: `setup.xml`, located alongside its sub-folders
- Scenarios can be split across multiple files in nested sub-folders
- Each scenario contains: target URL, HTTP method, request headers, JSON body (as CDATA), expected response fields (JSONPath expressions + expected values), expected headers
- Recurring properties (URL, method, headers, body, expected values) can be defined in enclosing XML structures and are inherited or selectively overridden by child elements

For questions about the XML format, consult `doc/manual_en.adoc` or `doc/manual_de.adoc` rather than guessing from the code.

### Reporting

Report pipeline: raw results → `report.xml` → (XSLT) → `report.adoc` → (AsciidoctorJ) → `report.html` / `report.pdf`

Key resources in `src/main/resources/com/github/kreutzr/responsediff/reporter/`:
- `report-to-adoc.xslt` — transforms result XML to AsciiDoc
- `custom-html-overrides.html` — optional HTML customization
- `_custom-pdf-theme.yml` — PDF theme for AsciidoctorJ-PDF

Entry points: `AsciiDocConverter.toHtml()` / `AsciiDocConverter.toPdf()`

### Test Infrastructure

Tests extend `TestBase`, which initializes a `ResponseDiff` instance with a mock configuration and Mockito-backed HTTP responses. Service URLs used in tests: `http://candidate/`, `http://reference/`, `http://control/`.

Key test classes: `JsonDiffTest`, `HttpHandlerTest`, `DiffResponseFilterTest`, `ExecutionContextHelperTest`.

### Key Dependencies

| Library | Purpose |
|---|---|
| `java.net.http` | HTTP execution |
| Jackson 2.22.0 | JSON processing |
| JsonPath (Jayway) 3.0.0 | JSONPath queries |
| `javax.xml.transform` | XSLT processing (XML → AsciiDoc) |
| AsciidoctorJ 3.0.1 | AsciiDoc → HTML/PDF |
| Jakarta JAXB 4.0.8 | XML binding |
| Log4j 2 / SLF4j | Logging (`src/main/resources/log4j2.xml`) |
| JUnit 5 (Jupiter) 6.1.0 + Mockito 5 + AssertJ | Testing |

## Architecture Decisions

- **XML over YAML/JSON for scenarios:** Allows CDATA blocks for multi-line JSON bodies without escaping issues.
- **XSLT for reporting:** Transformation is declarative and stays in the XML domain; no intermediate Java object model required.
- **AsciidoctorJ over direct HTML templating:** Enables professional PDF output without a separate renderer.
- **JSONPath:** Allows precise referencing of actual/expected differences; supports machine-readable evaluation.
- **XSD + JAXB-generated classes:** Classes related to XML handling that are generated from the XSD are prefixed with `Xml` to avoid name clashes with operational class names.
- **Conservative Java version target:** Maximizes compatibility with older Java installations.

## Guidance for Claude Code

- For questions about the XML scenario format, read `doc/manual_en.adoc` (or `doc/manual_de.adoc`) or inspect test setups under `src/test/resources/com/github/kreutzr/responsediff/*/setup.xml`. Do not guess from the code.
- The XSLT stylesheets are intentionally complex; deviations from typical Java patterns are deliberate.
- Do not run `git` commands. The repository is under the owner's control.
- Avoid broad refactoring. Work on as few files as possible per sub-task.
- Work outward from the entry points listed above; follow dependencies deliberately rather than reading all files.
- The coding style is largely consistent. Some intentional deviations from common patterns exist to improve human readability — do not "fix" them.

## What Not to Evaluate

- **Security:** Libraries are current; security is not a primary goal of this tool.
- **Performance:** Test runs are not latency-critical.
- **XSLT optimization:** The reporting pipeline is sufficiently flexible as-is.
- **Coding style normalization:** Intentional style choices aid readability and should not be changed.

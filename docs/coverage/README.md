# API Coverage Reports

This directory contains generated API coverage reports for the awtea project, which tracks the implementation status of Java AWT APIs in the TeaVM-based awtea library.

## About

The `ApiDiff` utility compares awtea classes (prefixed with `T`) against standard Java AWT classes to measure implementation completeness. The reports show:

- **Overall coverage statistics** (global and per-package)
- **Per-class breakdowns** of implemented vs. missing methods, fields, and constructors
- **Visual progress indicators** showing implementation percentages

## Generating Reports

### Prerequisites

Build the project first:
```bash
./gradlew build
```

### HTML Report

Generate an interactive HTML report with expandable sections and color-coded indicators:

```bash
java -cp build/classes/java/main me.mdbell.awtea.util.ApiDiff --format html
```

This creates `docs/coverage/report.html` by default.

**Custom output path:**
```bash
java -cp build/classes/java/main me.mdbell.awtea.util.ApiDiff --format html --output my-report.html
```

### Markdown Report

Generate a Markdown report suitable for GitHub Pages, wikis, or version control:

```bash
java -cp build/classes/java/main me.mdbell.awtea.util.ApiDiff --format markdown
```

This creates `docs/coverage/report.md` by default.

**Custom output path:**
```bash
java -cp build/classes/java/main me.mdbell.awtea.util.ApiDiff --format markdown --output coverage.md
```

### Console Output

For quick checks, run without any format option to see console output:

```bash
java -cp build/classes/java/main me.mdbell.awtea.util.ApiDiff
```

### Filtering by Class

You can limit the analysis to specific classes:

```bash
java -cp build/classes/java/main me.mdbell.awtea.util.ApiDiff --format html java.awt.Graphics
```

## Report Features

### HTML Report
- **Interactive UI**: Expandable/collapsible sections for packages and classes
- **Color-coded badges**: Green (100%), Yellow (50-99%), Red (<50%)
- **Progress bars**: Visual representation of coverage percentages
- **Organized sections**: Separate listings for methods, fields, and constructors
- **Professional styling**: Clean, modern CSS design

### Markdown Report
- **GitHub-compatible**: Uses standard Markdown syntax
- **Shield badges**: Shows coverage badges using shields.io
- **ASCII progress bars**: Visual progress indicators in plain text
- **Diff-friendly**: Structured format for tracking changes over time
- **Code blocks**: Properly formatted method/field signatures

## Example Usage in CI/CD

Add to your CI workflow to track coverage over time:

```yaml
- name: Generate Coverage Report
  run: |
    ./gradlew build
    java -cp build/classes/java/main me.mdbell.awtea.util.ApiDiff --format html
    java -cp build/classes/java/main me.mdbell.awtea.util.ApiDiff --format markdown

- name: Upload Reports
  uses: actions/upload-artifact@v3
  with:
    name: coverage-reports
    path: docs/coverage/
```

## Understanding the Reports

### Coverage Metrics

- **Implemented**: Methods, fields, and constructors that exist in awtea
- **Missing**: Methods, fields, and constructors that exist in Java AWT but not in awtea
- **Total Coverage**: (Implemented / Total) × 100%

### Package Organization

Reports are organized by Java package (e.g., `java.applet`, `java.awt`, `javax.swing`)

### Class Status

- **Full Coverage (100%)**: All members are implemented
- **Partial Coverage (>0%)**: Some members are implemented
- **Missing (0%)**: No members are implemented

## Files

- `report.html` - Latest HTML coverage report
- `report.md` - Latest Markdown coverage report
- `README.md` - This documentation file

## Notes

- Reports are regenerated each time you run the ApiDiff utility
- Some classes may show errors during generation if they depend on TeaVM-specific APIs not available at runtime
- The coverage percentage reflects public and protected API members only

# API Coverage Reports

This directory contains generated API coverage reports for the awtea project, which tracks the implementation status of Java AWT APIs in the TeaVM-based awtea library.

## About

The `ApiDiff` utility compares awtea classes (prefixed with `T`) against standard Java AWT classes to measure implementation completeness. The reports show:

- **Overall coverage statistics** (global and per-package)
- **Per-class breakdowns** of implemented vs. missing methods, fields, and constructors
- **Visual progress indicators** showing implementation percentages

## Generating Reports

### Using Gradle Task (Recommended)

The simplest way to generate both HTML and Markdown reports:

```bash
./gradlew generateDocs
```

This generates both `docs/coverage/report.html` and multiple Markdown files in a single command:
- `report.md` - High-level summary index
- `<package>.md` - Index for each package
- `<package>_<class>.md` - Detailed report for each class

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

This creates multiple Markdown files in `docs/coverage/`:
- `report.md` - High-level summary with package table
- One file per package with class listings
- One file per class with detailed coverage information

All files are linked together for easy navigation.

**Custom output path:**
```bash
java -cp build/classes/java/main me.mdbell.awtea.util.ApiDiff --format markdown --output custom-dir/report.md
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
- **Multi-file structure**: Separate files per package and class for better navigation
- **High-level summary**: Index page with package table for quick overview
- **GitHub-compatible**: Uses standard Markdown syntax
- **Shield badges**: Shows coverage badges using shields.io
- **ASCII progress bars**: Visual progress indicators in plain text
- **Linked navigation**: Easy navigation between index, packages, and classes
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

### HTML Report
- `report.html` - Interactive single-file HTML coverage report

### Markdown Reports
- `report.md` - High-level summary index with package table
- `<package>.md` - Index for each package (e.g., `java_awt.md`)
- `<package>_<class>.md` - Detailed report for each class (e.g., `java_awt_Graphics.md`)
- `README.md` - This documentation file

## Notes

- Reports are regenerated each time you run the ApiDiff utility or `./gradlew generateDocs`
- Markdown reports are split into multiple files for better navigation and reduced file sizes
- All Markdown files are linked together - start from `report.md` for the overview
- Some classes may show errors during generation if they depend on TeaVM-specific APIs not available at runtime
- The coverage percentage reflects public and protected API members only

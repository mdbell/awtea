# Class: `NumberFormat` ![Coverage](https://img.shields.io/badge/coverage-81.4%25-green)

**Full Name:** `java.text.NumberFormat`

**Coverage:** 35 / 43 (81.4%)

```
[████████████████████████████████████████░░░░░░░░░░] 81.4%
```

## ✓ Implemented Methods

- `public abstract java.lang.Number parse(java.lang.String, java.text.ParsePosition)`
- `public abstract java.lang.StringBuffer format(double, java.lang.StringBuffer, java.text.FieldPosition)`
- `public abstract java.lang.StringBuffer format(long, java.lang.StringBuffer, java.text.FieldPosition)`
- `public boolean equals(java.lang.Object)`
- `public boolean isGroupingUsed()`
- `public boolean isParseIntegerOnly()`
- `public final java.lang.Object parseObject(java.lang.String, java.text.ParsePosition)`
- `public final java.lang.String format(double)`
- `public final java.lang.String format(long)`
- `public int getMaximumFractionDigits()`
- `public int getMaximumIntegerDigits()`
- `public int getMinimumFractionDigits()`
- `public int getMinimumIntegerDigits()`
- `public int hashCode()`
- `public java.lang.Number parse(java.lang.String)`
- `public java.lang.Object clone()`
- `public java.lang.StringBuffer format(java.lang.Object, java.lang.StringBuffer, java.text.FieldPosition)`
- `public java.math.RoundingMode getRoundingMode()`
- `public java.util.Currency getCurrency()`
- `public static java.text.NumberFormat getCurrencyInstance(java.util.Locale)`
- `public static java.text.NumberFormat getInstance(java.util.Locale)`
- `public static java.text.NumberFormat getIntegerInstance(java.util.Locale)`
- `public static java.text.NumberFormat getNumberInstance(java.util.Locale)`
- `public static java.text.NumberFormat getPercentInstance(java.util.Locale)`
- `public static java.util.Locale[] getAvailableLocales()`
- `public void setCurrency(java.util.Currency)`
- `public void setGroupingUsed(boolean)`
- `public void setMaximumFractionDigits(int)`
- `public void setMaximumIntegerDigits(int)`
- `public void setMinimumFractionDigits(int)`
- `public void setMinimumIntegerDigits(int)`
- `public void setParseIntegerOnly(boolean)`
- `public void setRoundingMode(java.math.RoundingMode)`

## ✗ Missing Methods

- `public static final java.text.NumberFormat getCurrencyInstance()`
- `public static final java.text.NumberFormat getInstance()`
- `public static final java.text.NumberFormat getIntegerInstance()`
- `public static final java.text.NumberFormat getNumberInstance()`
- `public static final java.text.NumberFormat getPercentInstance()`
- `public static java.text.NumberFormat getCompactNumberInstance()`
- `public static java.text.NumberFormat getCompactNumberInstance(java.util.Locale, java.text.NumberFormat$Style)`

## ✓ Implemented Fields

- `public static final int FRACTION_FIELD`
- `public static final int INTEGER_FIELD`

## ✗ Missing Constructors

- `protected java.text.NumberFormat()`


[← Back to Package](index.md)

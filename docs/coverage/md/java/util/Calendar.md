# Class: `Calendar` ![Coverage](https://img.shields.io/badge/coverage-83.9%25-green)

**Full Name:** `java.util.Calendar`

**Coverage:** 94 / 112 (83.9%)

```
[█████████████████████████████████████████░░░░░░░░░] 83.9%
```

## ✓ Implemented Methods

- `protected abstract void computeFields()`
- `protected abstract void computeTime()`
- `protected final int internalGet(int)`
- `protected void complete()`
- `public abstract int getGreatestMinimum(int)`
- `public abstract int getLeastMaximum(int)`
- `public abstract int getMaximum(int)`
- `public abstract int getMinimum(int)`
- `public abstract void add(int, int)`
- `public abstract void roll(int, boolean)`
- `public boolean after(java.lang.Object)`
- `public boolean before(java.lang.Object)`
- `public boolean equals(java.lang.Object)`
- `public boolean isLenient()`
- `public final boolean isSet(int)`
- `public final java.util.Date getTime()`
- `public final void clear()`
- `public final void clear(int)`
- `public final void set(int, int, int)`
- `public final void set(int, int, int, int, int)`
- `public final void set(int, int, int, int, int, int)`
- `public final void setTime(java.util.Date)`
- `public int compareTo(java.lang.Object)`
- `public int compareTo(java.util.Calendar)`
- `public int get(int)`
- `public int getActualMaximum(int)`
- `public int getActualMinimum(int)`
- `public int getFirstDayOfWeek()`
- `public int getMinimalDaysInFirstWeek()`
- `public int hashCode()`
- `public java.lang.Object clone()`
- `public java.lang.String toString()`
- `public java.util.TimeZone getTimeZone()`
- `public long getTimeInMillis()`
- `public static java.util.Calendar getInstance()`
- `public static java.util.Calendar getInstance(java.util.Locale)`
- `public static java.util.Calendar getInstance(java.util.TimeZone)`
- `public static java.util.Calendar getInstance(java.util.TimeZone, java.util.Locale)`
- `public static java.util.Locale[] getAvailableLocales()`
- `public void roll(int, int)`
- `public void set(int, int)`
- `public void setFirstDayOfWeek(int)`
- `public void setLenient(boolean)`
- `public void setMinimalDaysInFirstWeek(int)`
- `public void setTimeInMillis(long)`
- `public void setTimeZone(java.util.TimeZone)`

## ✗ Missing Methods

- `public boolean isWeekDateSupported()`
- `public final java.time.Instant toInstant()`
- `public int getWeekYear()`
- `public int getWeeksInWeekYear()`
- `public java.lang.String getCalendarType()`
- `public java.lang.String getDisplayName(int, int, java.util.Locale)`
- `public java.util.Map getDisplayNames(int, int, java.util.Locale)`
- `public static java.util.Set getAvailableCalendarTypes()`
- `public void setWeekDate(int, int, int)`

## ✓ Implemented Fields

- `protected boolean areFieldsSet`
- `protected boolean isTimeSet`
- `protected boolean[] isSet`
- `protected int[] fields`
- `protected long time`
- `public static final int AM`
- `public static final int AM_PM`
- `public static final int APRIL`
- `public static final int AUGUST`
- `public static final int DATE`
- `public static final int DAY_OF_MONTH`
- `public static final int DAY_OF_WEEK`
- `public static final int DAY_OF_WEEK_IN_MONTH`
- `public static final int DAY_OF_YEAR`
- `public static final int DECEMBER`
- `public static final int DST_OFFSET`
- `public static final int ERA`
- `public static final int FEBRUARY`
- `public static final int FIELD_COUNT`
- `public static final int FRIDAY`
- `public static final int HOUR`
- `public static final int HOUR_OF_DAY`
- `public static final int JANUARY`
- `public static final int JULY`
- `public static final int JUNE`
- `public static final int MARCH`
- `public static final int MAY`
- `public static final int MILLISECOND`
- `public static final int MINUTE`
- `public static final int MONDAY`
- `public static final int MONTH`
- `public static final int NOVEMBER`
- `public static final int OCTOBER`
- `public static final int PM`
- `public static final int SATURDAY`
- `public static final int SECOND`
- `public static final int SEPTEMBER`
- `public static final int SUNDAY`
- `public static final int THURSDAY`
- `public static final int TUESDAY`
- `public static final int UNDECIMBER`
- `public static final int WEDNESDAY`
- `public static final int WEEK_OF_MONTH`
- `public static final int WEEK_OF_YEAR`
- `public static final int YEAR`
- `public static final int ZONE_OFFSET`

## ✗ Missing Fields

- `public static final int ALL_STYLES`
- `public static final int LONG`
- `public static final int LONG_FORMAT`
- `public static final int LONG_STANDALONE`
- `public static final int NARROW_FORMAT`
- `public static final int NARROW_STANDALONE`
- `public static final int SHORT`
- `public static final int SHORT_FORMAT`
- `public static final int SHORT_STANDALONE`

## ✓ Implemented Constructors

- `protected java.util.Calendar()`
- `protected java.util.Calendar(java.util.TimeZone, java.util.Locale)`


[← Back to Package](index.md)

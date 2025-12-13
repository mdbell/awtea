# Class: `ConcurrentHashMap` ![Coverage](https://img.shields.io/badge/coverage-37.1%25-orange)

**Full Name:** `java.util.concurrent.ConcurrentHashMap`

**Coverage:** 26 / 70 (37.1%)

```
[██████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 37.1%
```

## ✓ Implemented Methods

- `public boolean containsKey(java.lang.Object)`
- `public boolean containsValue(java.lang.Object)`
- `public boolean isEmpty()`
- `public boolean remove(java.lang.Object, java.lang.Object)`
- `public boolean replace(java.lang.Object, java.lang.Object, java.lang.Object)`
- `public int size()`
- `public java.lang.Object compute(java.lang.Object, java.util.function.BiFunction)`
- `public java.lang.Object computeIfAbsent(java.lang.Object, java.util.function.Function)`
- `public java.lang.Object computeIfPresent(java.lang.Object, java.util.function.BiFunction)`
- `public java.lang.Object get(java.lang.Object)`
- `public java.lang.Object getOrDefault(java.lang.Object, java.lang.Object)`
- `public java.lang.Object put(java.lang.Object, java.lang.Object)`
- `public java.lang.Object putIfAbsent(java.lang.Object, java.lang.Object)`
- `public java.lang.Object remove(java.lang.Object)`
- `public java.lang.Object replace(java.lang.Object, java.lang.Object)`
- `public java.util.Collection values()`
- `public java.util.Set entrySet()`
- `public java.util.Set keySet()`
- `public void clear()`
- `public void forEach(java.util.function.BiConsumer)`
- `public void putAll(java.util.Map)`
- `public void replaceAll(java.util.function.BiFunction)`

## ✗ Missing Methods

- `public boolean contains(java.lang.Object)`
- `public boolean equals(java.lang.Object)`
- `public double reduceEntriesToDouble(long, java.util.function.ToDoubleFunction, double, java.util.function.DoubleBinaryOperator)`
- `public double reduceKeysToDouble(long, java.util.function.ToDoubleFunction, double, java.util.function.DoubleBinaryOperator)`
- `public double reduceToDouble(long, java.util.function.ToDoubleBiFunction, double, java.util.function.DoubleBinaryOperator)`
- `public double reduceValuesToDouble(long, java.util.function.ToDoubleFunction, double, java.util.function.DoubleBinaryOperator)`
- `public int hashCode()`
- `public int reduceEntriesToInt(long, java.util.function.ToIntFunction, int, java.util.function.IntBinaryOperator)`
- `public int reduceKeysToInt(long, java.util.function.ToIntFunction, int, java.util.function.IntBinaryOperator)`
- `public int reduceToInt(long, java.util.function.ToIntBiFunction, int, java.util.function.IntBinaryOperator)`
- `public int reduceValuesToInt(long, java.util.function.ToIntFunction, int, java.util.function.IntBinaryOperator)`
- `public java.lang.Object merge(java.lang.Object, java.lang.Object, java.util.function.BiFunction)`
- `public java.lang.Object reduce(long, java.util.function.BiFunction, java.util.function.BiFunction)`
- `public java.lang.Object reduceEntries(long, java.util.function.Function, java.util.function.BiFunction)`
- `public java.lang.Object reduceKeys(long, java.util.function.BiFunction)`
- `public java.lang.Object reduceKeys(long, java.util.function.Function, java.util.function.BiFunction)`
- `public java.lang.Object reduceValues(long, java.util.function.BiFunction)`
- `public java.lang.Object reduceValues(long, java.util.function.Function, java.util.function.BiFunction)`
- `public java.lang.Object search(long, java.util.function.BiFunction)`
- `public java.lang.Object searchEntries(long, java.util.function.Function)`
- `public java.lang.Object searchKeys(long, java.util.function.Function)`
- `public java.lang.Object searchValues(long, java.util.function.Function)`
- `public java.lang.String toString()`
- `public java.util.Enumeration elements()`
- `public java.util.Enumeration keys()`
- `public java.util.Map$Entry reduceEntries(long, java.util.function.BiFunction)`
- `public java.util.concurrent.ConcurrentHashMap$KeySetView keySet()`
- `public java.util.concurrent.ConcurrentHashMap$KeySetView keySet(java.lang.Object)`
- `public long mappingCount()`
- `public long reduceEntriesToLong(long, java.util.function.ToLongFunction, long, java.util.function.LongBinaryOperator)`
- `public long reduceKeysToLong(long, java.util.function.ToLongFunction, long, java.util.function.LongBinaryOperator)`
- `public long reduceToLong(long, java.util.function.ToLongBiFunction, long, java.util.function.LongBinaryOperator)`
- `public long reduceValuesToLong(long, java.util.function.ToLongFunction, long, java.util.function.LongBinaryOperator)`
- `public static java.util.concurrent.ConcurrentHashMap$KeySetView newKeySet()`
- `public static java.util.concurrent.ConcurrentHashMap$KeySetView newKeySet(int)`
- `public void forEach(long, java.util.function.BiConsumer)`
- `public void forEach(long, java.util.function.BiFunction, java.util.function.Consumer)`
- `public void forEachEntry(long, java.util.function.Consumer)`
- `public void forEachEntry(long, java.util.function.Function, java.util.function.Consumer)`
- `public void forEachKey(long, java.util.function.Consumer)`
- `public void forEachKey(long, java.util.function.Function, java.util.function.Consumer)`
- `public void forEachValue(long, java.util.function.Consumer)`
- `public void forEachValue(long, java.util.function.Function, java.util.function.Consumer)`

## ✓ Implemented Constructors

- `public java.util.concurrent.ConcurrentHashMap()`
- `public java.util.concurrent.ConcurrentHashMap(int)`
- `public java.util.concurrent.ConcurrentHashMap(int, float)`
- `public java.util.concurrent.ConcurrentHashMap(java.util.Map)`

## ✗ Missing Constructors

- `public java.util.concurrent.ConcurrentHashMap(int, float, int)`


[← Back to Package](index.md)

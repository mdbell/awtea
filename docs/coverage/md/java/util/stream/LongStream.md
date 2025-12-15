# Class: `LongStream` ![Coverage](https://img.shields.io/badge/coverage-88.0%25-green)

**Full Name:** `java.util.stream.LongStream`

**Coverage:** 44 / 50 (88.0%)

```
[████████████████████████████████████████████░░░░░░] 88.0%
```

## ✓ Implemented Methods

- `public abstract boolean allMatch(java.util.function.LongPredicate)`
- `public abstract boolean anyMatch(java.util.function.LongPredicate)`
- `public abstract boolean noneMatch(java.util.function.LongPredicate)`
- `public abstract java.lang.Object collect(java.util.function.Supplier, java.util.function.ObjLongConsumer, java.util.function.BiConsumer)`
- `public abstract java.util.LongSummaryStatistics summaryStatistics()`
- `public abstract java.util.OptionalDouble average()`
- `public abstract java.util.OptionalLong findAny()`
- `public abstract java.util.OptionalLong findFirst()`
- `public abstract java.util.OptionalLong max()`
- `public abstract java.util.OptionalLong min()`
- `public abstract java.util.OptionalLong reduce(java.util.function.LongBinaryOperator)`
- `public abstract java.util.PrimitiveIterator$OfLong iterator()`
- `public abstract java.util.Spliterator$OfLong spliterator()`
- `public abstract java.util.stream.DoubleStream asDoubleStream()`
- `public abstract java.util.stream.DoubleStream mapToDouble(java.util.function.LongToDoubleFunction)`
- `public abstract java.util.stream.IntStream mapToInt(java.util.function.LongToIntFunction)`
- `public abstract java.util.stream.LongStream distinct()`
- `public abstract java.util.stream.LongStream filter(java.util.function.LongPredicate)`
- `public abstract java.util.stream.LongStream flatMap(java.util.function.LongFunction)`
- `public abstract java.util.stream.LongStream limit(long)`
- `public abstract java.util.stream.LongStream map(java.util.function.LongUnaryOperator)`
- `public abstract java.util.stream.LongStream peek(java.util.function.LongConsumer)`
- `public abstract java.util.stream.LongStream skip(long)`
- `public abstract java.util.stream.LongStream sorted()`
- `public abstract java.util.stream.Stream boxed()`
- `public abstract java.util.stream.Stream mapToObj(java.util.function.LongFunction)`
- `public abstract long count()`
- `public abstract long reduce(long, java.util.function.LongBinaryOperator)`
- `public abstract long sum()`
- `public abstract long[] toArray()`
- `public abstract void forEach(java.util.function.LongConsumer)`
- `public abstract void forEachOrdered(java.util.function.LongConsumer)`
- `public java.util.Iterator iterator()`
- `public java.util.Spliterator spliterator()`
- `public static java.util.stream.LongStream concat(java.util.stream.LongStream, java.util.stream.LongStream)`
- `public static java.util.stream.LongStream empty()`
- `public static java.util.stream.LongStream generate(java.util.function.LongSupplier)`
- `public static java.util.stream.LongStream iterate(long, java.util.function.LongPredicate, java.util.function.LongUnaryOperator)`
- `public static java.util.stream.LongStream iterate(long, java.util.function.LongUnaryOperator)`
- `public static java.util.stream.LongStream of(long)`
- `public static java.util.stream.LongStream of(long[])`
- `public static java.util.stream.LongStream range(long, long)`
- `public static java.util.stream.LongStream rangeClosed(long, long)`
- `public static java.util.stream.LongStream$Builder builder()`

## ✗ Missing Methods

- `public abstract java.util.stream.LongStream parallel()`
- `public abstract java.util.stream.LongStream sequential()`
- `public java.util.stream.BaseStream parallel()`
- `public java.util.stream.BaseStream sequential()`
- `public java.util.stream.LongStream dropWhile(java.util.function.LongPredicate)`
- `public java.util.stream.LongStream takeWhile(java.util.function.LongPredicate)`


[← Back to Package](index.md)

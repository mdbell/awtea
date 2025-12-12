# Class: `DoubleStream` ![Coverage](https://img.shields.io/badge/coverage-87.5%25-green)

**Full Name:** `java.util.stream.DoubleStream`

**Coverage:** 42 / 48 (87.5%)

```
[███████████████████████████████████████████░░░░░░░] 87.5%
```

## ✓ Implemented Methods

- `public abstract boolean allMatch(java.util.function.DoublePredicate)`
- `public abstract boolean anyMatch(java.util.function.DoublePredicate)`
- `public abstract boolean noneMatch(java.util.function.DoublePredicate)`
- `public abstract double reduce(double, java.util.function.DoubleBinaryOperator)`
- `public abstract double sum()`
- `public abstract double[] toArray()`
- `public abstract java.lang.Object collect(java.util.function.Supplier, java.util.function.ObjDoubleConsumer, java.util.function.BiConsumer)`
- `public abstract java.util.DoubleSummaryStatistics summaryStatistics()`
- `public abstract java.util.OptionalDouble average()`
- `public abstract java.util.OptionalDouble findAny()`
- `public abstract java.util.OptionalDouble findFirst()`
- `public abstract java.util.OptionalDouble max()`
- `public abstract java.util.OptionalDouble min()`
- `public abstract java.util.OptionalDouble reduce(java.util.function.DoubleBinaryOperator)`
- `public abstract java.util.PrimitiveIterator$OfDouble iterator()`
- `public abstract java.util.Spliterator$OfDouble spliterator()`
- `public abstract java.util.stream.DoubleStream distinct()`
- `public abstract java.util.stream.DoubleStream filter(java.util.function.DoublePredicate)`
- `public abstract java.util.stream.DoubleStream flatMap(java.util.function.DoubleFunction)`
- `public abstract java.util.stream.DoubleStream limit(long)`
- `public abstract java.util.stream.DoubleStream map(java.util.function.DoubleUnaryOperator)`
- `public abstract java.util.stream.DoubleStream peek(java.util.function.DoubleConsumer)`
- `public abstract java.util.stream.DoubleStream skip(long)`
- `public abstract java.util.stream.DoubleStream sorted()`
- `public abstract java.util.stream.IntStream mapToInt(java.util.function.DoubleToIntFunction)`
- `public abstract java.util.stream.LongStream mapToLong(java.util.function.DoubleToLongFunction)`
- `public abstract java.util.stream.Stream boxed()`
- `public abstract java.util.stream.Stream mapToObj(java.util.function.DoubleFunction)`
- `public abstract long count()`
- `public abstract void forEach(java.util.function.DoubleConsumer)`
- `public abstract void forEachOrdered(java.util.function.DoubleConsumer)`
- `public java.util.Iterator iterator()`
- `public java.util.Spliterator spliterator()`
- `public java.util.stream.DoubleStream mapMulti(java.util.stream.DoubleStream$DoubleMapMultiConsumer)`
- `public static java.util.stream.DoubleStream concat(java.util.stream.DoubleStream, java.util.stream.DoubleStream)`
- `public static java.util.stream.DoubleStream empty()`
- `public static java.util.stream.DoubleStream generate(java.util.function.DoubleSupplier)`
- `public static java.util.stream.DoubleStream iterate(double, java.util.function.DoublePredicate, java.util.function.DoubleUnaryOperator)`
- `public static java.util.stream.DoubleStream iterate(double, java.util.function.DoubleUnaryOperator)`
- `public static java.util.stream.DoubleStream of(double)`
- `public static java.util.stream.DoubleStream of(double[])`
- `public static java.util.stream.DoubleStream$Builder builder()`

## ✗ Missing Methods

- `public abstract java.util.stream.DoubleStream parallel()`
- `public abstract java.util.stream.DoubleStream sequential()`
- `public java.util.stream.BaseStream parallel()`
- `public java.util.stream.BaseStream sequential()`
- `public java.util.stream.DoubleStream dropWhile(java.util.function.DoublePredicate)`
- `public java.util.stream.DoubleStream takeWhile(java.util.function.DoublePredicate)`


[← Back to Package](index.md)

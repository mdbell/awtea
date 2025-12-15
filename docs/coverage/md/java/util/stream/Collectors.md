# Class: `Collectors` ![Coverage](https://img.shields.io/badge/coverage-86.0%25-green)

**Full Name:** `java.util.stream.Collectors`

**Coverage:** 37 / 43 (86.0%)

```
[███████████████████████████████████████████░░░░░░░] 86.0%
```

## ✓ Implemented Methods

- `public static java.util.stream.Collector averagingDouble(java.util.function.ToDoubleFunction)`
- `public static java.util.stream.Collector averagingInt(java.util.function.ToIntFunction)`
- `public static java.util.stream.Collector averagingLong(java.util.function.ToLongFunction)`
- `public static java.util.stream.Collector collectingAndThen(java.util.stream.Collector, java.util.function.Function)`
- `public static java.util.stream.Collector counting()`
- `public static java.util.stream.Collector filtering(java.util.function.Predicate, java.util.stream.Collector)`
- `public static java.util.stream.Collector flatMapping(java.util.function.Function, java.util.stream.Collector)`
- `public static java.util.stream.Collector groupingBy(java.util.function.Function)`
- `public static java.util.stream.Collector groupingBy(java.util.function.Function, java.util.function.Supplier, java.util.stream.Collector)`
- `public static java.util.stream.Collector groupingBy(java.util.function.Function, java.util.stream.Collector)`
- `public static java.util.stream.Collector joining()`
- `public static java.util.stream.Collector joining(java.lang.CharSequence)`
- `public static java.util.stream.Collector joining(java.lang.CharSequence, java.lang.CharSequence, java.lang.CharSequence)`
- `public static java.util.stream.Collector mapping(java.util.function.Function, java.util.stream.Collector)`
- `public static java.util.stream.Collector maxBy(java.util.Comparator)`
- `public static java.util.stream.Collector minBy(java.util.Comparator)`
- `public static java.util.stream.Collector partitioningBy(java.util.function.Predicate)`
- `public static java.util.stream.Collector partitioningBy(java.util.function.Predicate, java.util.stream.Collector)`
- `public static java.util.stream.Collector reducing(java.lang.Object, java.util.function.BinaryOperator)`
- `public static java.util.stream.Collector reducing(java.lang.Object, java.util.function.Function, java.util.function.BinaryOperator)`
- `public static java.util.stream.Collector reducing(java.util.function.BinaryOperator)`
- `public static java.util.stream.Collector summarizingDouble(java.util.function.ToDoubleFunction)`
- `public static java.util.stream.Collector summarizingInt(java.util.function.ToIntFunction)`
- `public static java.util.stream.Collector summarizingLong(java.util.function.ToLongFunction)`
- `public static java.util.stream.Collector summingDouble(java.util.function.ToDoubleFunction)`
- `public static java.util.stream.Collector summingInt(java.util.function.ToIntFunction)`
- `public static java.util.stream.Collector summingLong(java.util.function.ToLongFunction)`
- `public static java.util.stream.Collector toCollection(java.util.function.Supplier)`
- `public static java.util.stream.Collector toList()`
- `public static java.util.stream.Collector toMap(java.util.function.Function, java.util.function.Function)`
- `public static java.util.stream.Collector toMap(java.util.function.Function, java.util.function.Function, java.util.function.BinaryOperator)`
- `public static java.util.stream.Collector toMap(java.util.function.Function, java.util.function.Function, java.util.function.BinaryOperator, java.util.function.Supplier)`
- `public static java.util.stream.Collector toSet()`
- `public static java.util.stream.Collector toUnmodifiableList()`
- `public static java.util.stream.Collector toUnmodifiableMap(java.util.function.Function, java.util.function.Function)`
- `public static java.util.stream.Collector toUnmodifiableMap(java.util.function.Function, java.util.function.Function, java.util.function.BinaryOperator)`
- `public static java.util.stream.Collector toUnmodifiableSet()`

## ✗ Missing Methods

- `public static java.util.stream.Collector groupingByConcurrent(java.util.function.Function)`
- `public static java.util.stream.Collector groupingByConcurrent(java.util.function.Function, java.util.function.Supplier, java.util.stream.Collector)`
- `public static java.util.stream.Collector groupingByConcurrent(java.util.function.Function, java.util.stream.Collector)`
- `public static java.util.stream.Collector toConcurrentMap(java.util.function.Function, java.util.function.Function)`
- `public static java.util.stream.Collector toConcurrentMap(java.util.function.Function, java.util.function.Function, java.util.function.BinaryOperator)`
- `public static java.util.stream.Collector toConcurrentMap(java.util.function.Function, java.util.function.Function, java.util.function.BinaryOperator, java.util.function.Supplier)`


[← Back to Package](index.md)

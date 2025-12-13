# Logging System

## Overview

The awtea project uses a unified logging framework that provides structured logging with configurable log levels and support for both native Java and TeaVM environments. The logging system is located in the `me.mdbell.awtea.util.logging` package.

## Key Components

### LogLevel
Enum defining log levels in order of severity:
- `ERROR` - Critical errors that need immediate attention
- `WARN` - Warning messages for potentially harmful situations
- `INFO` - Informational messages about normal operations
- `DEBUG` - Detailed debugging information

### Logger
Main interface for logging. Provides methods for each log level with support for:
- Simple messages: `logger.info("Message")`
- String formatting: `logger.info("User {} logged in", username)`
- Exception logging: `logger.error("Failed to connect", exception)`

### LoggerFactory
Factory class for obtaining logger instances:
```java
private static final Logger log = LoggerFactory.getLogger(MyClass.class);
```

### LogSink
Interface for pluggable log destinations. Built-in implementations:
- `ConsoleLogSink` - Routes logs to System.out/err or browser console
- `LogFrameSink` - Routes logs to the UI LogFrame component

## Usage

### Basic Logging

```java
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

public class MyClass {
    private static final Logger log = LoggerFactory.getLogger(MyClass.class);
    
    public void doSomething() {
        log.info("Starting operation");
        
        try {
            // ... operation ...
            log.debug("Processing item: {}", item);
        } catch (Exception e) {
            log.error("Operation failed", e);
        }
        
        log.info("Operation complete");
    }
}
```

### String Formatting

The logging framework supports SLF4J-style `{}` placeholder formatting:

```java
log.info("Processing {} items", count);
log.debug("User {} accessed resource {}", username, resource);
log.warn("Cache hit rate: {}%", hitRate * 100);
```

Multiple placeholders are replaced in order:

```java
log.info("User {} performed {} on {} at {}", user, action, resource, timestamp);
```

### Exception Logging

Always include exceptions with error/warn logs:

```java
try {
    // risky operation
} catch (IOException e) {
    log.error("Failed to read file: {}", filename, e);
}
```

### Conditional Logging

Check if a level is enabled before expensive string operations:

```java
if (log.isDebugEnabled()) {
    log.debug("Expensive debug info: {}", buildExpensiveDebugString());
}
```

### Configuring Log Level

#### Via System Property

Set the log level at startup using the system property:

```bash
# Set to DEBUG
java -Dme.mdbell.awtea.log.level=DEBUG -jar myapp.jar

# Set to ERROR
java -Dme.mdbell.awtea.log.level=ERROR -jar myapp.jar

# Valid values: ERROR, WARN, INFO (default), DEBUG
```

For Gradle:
```bash
./gradlew run -Dme.mdbell.awtea.log.level=DEBUG
```

#### Programmatically

Set the global log level at runtime:

```java
import me.mdbell.awtea.util.logging.LogLevel;
import me.mdbell.awtea.util.logging.LoggerFactory;

// Set to DEBUG to see all messages
LoggerFactory.setGlobalLevel(LogLevel.DEBUG);

// Set to ERROR to only see errors
LoggerFactory.setGlobalLevel(LogLevel.ERROR);
```

### Custom Log Sinks

Register custom sinks to capture logs:

```java
LogSink customSink = new LogSink() {
    @Override
    public boolean accepts(LogLevel level) {
        return level == LogLevel.ERROR;
    }
    
    @Override
    public void log(String logger, LogLevel level, String message, Throwable throwable) {
        // Custom handling - e.g., send to monitoring service
    }
};

LoggerFactory.addSink(customSink);
```

## Migration from System.out/err

### Replace System.out.println

Before:
```java
System.out.println("Starting process");
System.out.println("Processing: " + item);
```

After:
```java
private static final Logger log = LoggerFactory.getLogger(MyClass.class);

log.info("Starting process");
log.info("Processing: {}", item);
```

### Replace System.err.println

Before:
```java
System.err.println("Error occurred: " + error);
```

After:
```java
log.error("Error occurred: {}", error);
```

### Replace System.out.printf

Before:
```java
System.out.printf("Count: %d, Rate: %.2f%%\n", count, rate);
```

After:
```java
// Note: Our logging system uses SLF4J-style {} placeholders, not printf-style format strings
// For formatted numbers, format them first, then log:
log.info("Count: {}, Rate: {}%", count, String.format("%.2f", rate));

// Or use simple placeholders:
log.info("Count: {}, Rate: {}", count, rate);
```

### Replace printStackTrace

Before:
```java
try {
    // ...
} catch (Exception e) {
    e.printStackTrace();
}
```

After:
```java
try {
    // ...
} catch (Exception e) {
    log.error("Operation failed", e);
}
```

## TeaVM Compatibility

The logging system is fully compatible with TeaVM and will automatically route logs to the browser console with appropriate log levels:
- `log.error()` → `console.error()`
- `log.warn()` → `console.warn()`
- `log.info()` → `console.info()`
- `log.debug()` → `console.log()`

## Integration with LogFrame UI

When running in a browser with the LogFrame UI component, logs are automatically captured and displayed in the UI log viewer. The LogFrame provides:
- Color-coded log levels
- Timestamps for each log entry
- Scrollable history (up to 500 entries)
- Real-time updates

## Best Practices

1. **Use appropriate log levels**
   - `ERROR`: Errors that need immediate attention
   - `WARN`: Unexpected situations that don't prevent operation
   - `INFO`: Important business events (user actions, system state changes)
   - `DEBUG`: Detailed diagnostic information

2. **Use string formatting instead of concatenation**
   - Good: `log.info("User {} logged in", username)`
   - Bad: `log.info("User " + username + " logged in")`

3. **Include context in error messages**
   - Good: `log.error("Failed to save user {} to database", userId, e)`
   - Bad: `log.error("Database error", e)`

4. **Don't log sensitive information**
   - Avoid logging passwords, API keys, personal data

5. **Use guards for expensive operations**
   ```java
   if (log.isDebugEnabled()) {
       log.debug("Complex debug info: {}", buildComplexString());
   }
   ```

6. **Keep one logger per class**
   ```java
   private static final Logger log = LoggerFactory.getLogger(MyClass.class);
   ```

## Testing

The logging system can be tested by:
1. Registering test sinks to capture log messages
2. Verifying correct log levels are used
3. Checking that exceptions are properly logged

Example test sink:
```java
List<String> capturedLogs = new ArrayList<>();
LogSink testSink = new LogSink() {
    @Override
    public boolean accepts(LogLevel level) { return true; }
    
    @Override
    public void log(String logger, LogLevel level, String message, Throwable t) {
        capturedLogs.add(level.name() + ": " + message);
    }
};

LoggerFactory.addSink(testSink);
// ... run tests ...
LoggerFactory.removeSink(testSink);
```

## Future Enhancements

Potential future improvements:
- Per-class log level configuration
- Log message filtering by pattern
- Async logging support
- Log file output (for native Java)
- Structured logging (JSON format)
- MDC (Mapped Diagnostic Context) support

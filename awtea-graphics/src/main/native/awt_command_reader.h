/**
 * Variable-length command buffer reader
 * 
 * Commands are stored in the format:
 *   [opcode: uint8][flags: uint8][length: uint16][data: length*4 bytes]
 * 
 * The length field is in words (4-byte units) and does NOT include the 4-byte header.
 * All multi-byte values are in little-endian byte order.
 */
#pragma once

#include <stdint.h>
#include <stddef.h>
#include <string.h>
#include "awt_log.h"

// Buffer size: 16KB = 4096 words
#define COMMAND_BUFFER_SIZE_WORDS 4096

// Command flags
#define CMD_FLAG_EXTENDED 0x01  // Bit 0: Command is part of extended set (future use)

/**
 * Command reader state
 */
typedef struct {
    uint32_t* buffer;      // Pointer to command buffer (owned by this reader)
    size_t size_words;     // Buffer size in words (4-byte units)
    size_t pos;            // Current read position in bytes (absolute)
    size_t limit;          // Read limit in bytes (absolute, exclusive)
} CommandReader;

/**
 * Reset the reader to the beginning with a new limit.
 * 
 * @param reader The reader to reset
 * @param bytes_used Number of bytes that contain valid command data
 */
static inline void reset_reader(CommandReader* reader, size_t bytes_used) {
    reader->pos = 0;
    reader->limit = bytes_used;
}

/**
 * Check if there are at least 'bytes' available to read.
 * 
 * @param reader The reader
 * @param bytes Number of bytes needed
 * @return 1 if enough bytes available, 0 otherwise
 */
static inline int reader_has_bytes(const CommandReader* reader, size_t bytes) {
    return (reader->pos + bytes) <= reader->limit;
}

/**
 * Read an unsigned 8-bit value.
 * Returns 0 on bounds error (also logs error).
 */
static inline uint8_t read_u8(CommandReader* reader) {
    if (!reader_has_bytes(reader, 1)) {
        log_error("read_u8: buffer underflow at pos %zu (limit %zu)", reader->pos, reader->limit);
        return 0;
    }
    uint8_t* ptr = (uint8_t*)reader->buffer;
    uint8_t val = ptr[reader->pos];
    reader->pos += 1;
    return val;
}

/**
 * Read an unsigned 16-bit value (little-endian).
 * Returns 0 on bounds error (also logs error).
 */
static inline uint16_t read_u16(CommandReader* reader) {
    if (!reader_has_bytes(reader, 2)) {
        log_error("read_u16: buffer underflow at pos %zu (limit %zu)", reader->pos, reader->limit);
        return 0;
    }
    uint8_t* ptr = (uint8_t*)reader->buffer;
    uint16_t val = ptr[reader->pos] | ((uint16_t)ptr[reader->pos + 1] << 8);
    reader->pos += 2;
    return val;
}

/**
 * Read an unsigned 32-bit value (little-endian).
 * Returns 0 on bounds error (also logs error).
 */
static inline uint32_t read_u32(CommandReader* reader) {
    if (!reader_has_bytes(reader, 4)) {
        log_error("read_u32: buffer underflow at pos %zu (limit %zu)", reader->pos, reader->limit);
        return 0;
    }
    uint8_t* ptr = (uint8_t*)reader->buffer;
    uint32_t val = ptr[reader->pos] |
                   ((uint32_t)ptr[reader->pos + 1] << 8) |
                   ((uint32_t)ptr[reader->pos + 2] << 16) |
                   ((uint32_t)ptr[reader->pos + 3] << 24);
    reader->pos += 4;
    return val;
}

/**
 * Read a 32-bit float value (little-endian).
 * Returns 0.0f on bounds error (also logs error).
 */
static inline float read_float(CommandReader* reader) {
    if (!reader_has_bytes(reader, 4)) {
        log_error("read_float: buffer underflow at pos %zu (limit %zu)", reader->pos, reader->limit);
        return 0.0f;
    }
    uint32_t raw = read_u32(reader);
    reader->pos -= 4; // Undo the position increment from read_u32
    
    // Read as bytes for proper alignment
    uint8_t* ptr = (uint8_t*)reader->buffer;
    raw = ptr[reader->pos] |
          ((uint32_t)ptr[reader->pos + 1] << 8) |
          ((uint32_t)ptr[reader->pos + 2] << 16) |
          ((uint32_t)ptr[reader->pos + 3] << 24);
    reader->pos += 4;
    
    // Reinterpret bits as float
    union { uint32_t u; float f; } converter;
    converter.u = raw;
    return converter.f;
}

/**
 * Skip 'bytes' in the reader.
 * Returns 1 on success, 0 on bounds error (also logs error).
 */
static inline int reader_skip(CommandReader* reader, size_t bytes) {
    if (!reader_has_bytes(reader, bytes)) {
        log_error("reader_skip: cannot skip %zu bytes at pos %zu (limit %zu)", 
                  bytes, reader->pos, reader->limit);
        return 0;
    }
    reader->pos += bytes;
    return 1;
}

/**
 * Get the current read position in bytes.
 */
static inline size_t reader_position(const CommandReader* reader) {
    return reader->pos;
}

/**
 * Check if we've reached the end of the command data.
 */
static inline int reader_at_end(const CommandReader* reader) {
    return reader->pos >= reader->limit;
}

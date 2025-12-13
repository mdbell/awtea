package me.mdbell.awtea.font;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class ByteReader {
	private final byte[] data;
	private final int start;
	private final int limit;
	private int pos;

	public ByteReader(byte[] data) {
		this(data, 0, data.length);
	}

	public ByteReader(byte[] data, int offset, int length) {
		this.data = data;
		this.start = offset;
		this.limit = offset + length;
		this.pos = offset;
	}

	private void require(int bytes) {
		if (pos + bytes > limit) {
			throw new IndexOutOfBoundsException("Not enough data: need " + bytes);
		}
	}

	public int position() {
		return pos - start;
	}

	public void setPosition(int newPos) {
		int absolute = start + newPos;
		if (absolute < start || absolute > limit) {
			throw new IllegalArgumentException("Bad position: " + newPos);
		}
		this.pos = absolute;
	}

	public void skip(int bytes) {
		require(bytes);
		pos += bytes;
	}

	public byte readInt8() {
		require(1);
		return data[pos++];
	}

	public int readUInt8() {
		return readInt8() & 0xFF;
	}

	public short readInt16() {
		require(2);
		int b1 = data[pos++] & 0xFF;
		int b2 = data[pos++] & 0xFF;
		return (short) ((b1 << 8) | b2);
	}

	public int readUInt16() {
		return readInt16() & 0xFFFF;
	}

	public int readInt32() {
		require(4);
		int b1 = data[pos++] & 0xFF;
		int b2 = data[pos++] & 0xFF;
		int b3 = data[pos++] & 0xFF;
		int b4 = data[pos++] & 0xFF;
		return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
	}

	public long readUInt32() {
		return readInt32() & 0xFFFFFFFFL;
	}

	public long readInt64() {
		require(8);
		long b1 = data[pos++] & 0xFFL;
		long b2 = data[pos++] & 0xFFL;
		long b3 = data[pos++] & 0xFFL;
		long b4 = data[pos++] & 0xFFL;
		long b5 = data[pos++] & 0xFFL;
		long b6 = data[pos++] & 0xFFL;
		long b7 = data[pos++] & 0xFFL;
		long b8 = data[pos++] & 0xFFL;

		return (b1 << 56) |
			(b2 << 48) |
			(b3 << 40) |
			(b4 << 32) |
			(b5 << 24) |
			(b6 << 16) |
			(b7 <<  8) |
			b8;
	}


	/** Four-byte ASCII tag, e.g. "head" */
	public String readTag() {
		require(4);
		String s = new String(data, pos, 4, StandardCharsets.ISO_8859_1);
		pos += 4;
		return s;
	}

	public byte[] readBytes(int length) {
		require(length);
		byte[] out = new byte[length];
		System.arraycopy(data, pos, out, 0, length);
		pos += length;
		return out;
	}

	public String readString(int length, Charset charset) {
		require(length);
		String s = new String(data, pos, length, charset);
		pos += length;
		return s;
	}

	public String readUtf16BEString(int byteLength) {
		return readString(byteLength, StandardCharsets.UTF_16BE);
	}

	public String readAsciiString(int length) {
		return readString(length, StandardCharsets.ISO_8859_1);
	}

	/** Create a new reader “view” into this data at absolute offset. */
	public ByteReader forkAt(int absoluteOffset) {
		if (absoluteOffset < 0 || absoluteOffset > data.length) {
			throw new IllegalArgumentException("Bad fork offset: " + absoluteOffset);
		}
		return new ByteReader(data, absoluteOffset, data.length - absoluteOffset);
	}

	/** Create a new reader at (start + relativeOffset). */
	public ByteReader forkRelative(int relativeOffset) {
		return forkAt(start + relativeOffset);
	}
}

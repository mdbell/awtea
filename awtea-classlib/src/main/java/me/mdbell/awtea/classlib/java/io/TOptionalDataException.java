package me.mdbell.awtea.classlib.java.io;

import java.io.ObjectStreamException;

/**
 * @see java.io.OptionalDataException
 *
 * Missing from TeaVM's classlib. Required so catch clauses referencing it
 * compile to a valid module under wasm-gc: the wasm backend emits a phantom
 * struct type with no Throwable supertype for missing exception classes,
 * which fails module validation at instantiation (br_on_cast subtype check).
 * The JS backend silently tolerates the same gap.
 */
public class TOptionalDataException extends ObjectStreamException {

	/**
	 * The number of bytes of primitive data available to be read
	 * in the current buffer.
	 */
	public int length;

	/**
	 * True if there is no more data in the buffered part of the stream.
	 */
	public boolean eof;

	TOptionalDataException(int len) {
		length = len;
		eof = false;
	}

	TOptionalDataException(boolean end) {
		length = 0;
		eof = end;
	}
}

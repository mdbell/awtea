package me.mdbell.awtea.classlib.java.io;

import java.io.ObjectStreamException;

/**
 * @see java.io.StreamCorruptedException
 *
 * Missing from TeaVM's classlib. Required so catch clauses referencing it
 * compile to a valid module under wasm-gc: the wasm backend emits a phantom
 * struct type with no Throwable supertype for missing exception classes,
 * which fails module validation at instantiation (br_on_cast subtype check).
 * The JS backend silently tolerates the same gap.
 */
public class TStreamCorruptedException extends ObjectStreamException {

	public TStreamCorruptedException() {
		super();
	}

	public TStreamCorruptedException(String reason) {
		super(reason);
	}
}

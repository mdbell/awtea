package me.mdbell.awtea.classlib.java.awt.event;

import lombok.Getter;
import me.mdbell.awtea.classlib.java.awt.TAWTEvent;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.function.JSConsumer;

public class TInvocationEvent extends TAWTEvent implements TActiveEvent {

	public static final int INVOCATION_FIRST = 1200;

	public static final int INVOCATION_DEFAULT = INVOCATION_FIRST;

	public static final int INVOCATION_LAST = INVOCATION_DEFAULT;

	private final Runnable runnable;

	private final Object notifier;
	private final Runnable listener;
	private final boolean catchThrowables;

	@Getter
	private final long when;

	@Getter
	private boolean dispatched = false;

	@Getter
	private Throwable throwable;

	@Getter
	private Exception exception;

	private JSConsumer<Boolean> resolvePromise;

	@Getter
	private JSPromise<Boolean> promise;

	public TInvocationEvent(Object source, Runnable runnable) {
		this(source, INVOCATION_DEFAULT, runnable, null, null, false);
	}

	public TInvocationEvent(Object source, Runnable runnable, Object notifier, boolean catchThrowables) {
		this(source, INVOCATION_DEFAULT, runnable, notifier, null, catchThrowables);
	}

	public TInvocationEvent(Object source, int id, Runnable runnable, Runnable listener,
							boolean catchThrowables) {
		this(source, id, runnable, null, listener, catchThrowables);
	}

	protected TInvocationEvent(Object source, int id, Runnable runnable, Object notifier,
							   boolean catchThrowables) {
		this(source, id, runnable, notifier, null, catchThrowables);
	}

	private TInvocationEvent(Object source, int id, Runnable runnable, Object notifier,
							 Runnable listener, boolean catchThrowables) {
		super(source, id);
		this.runnable = runnable;
		this.notifier = notifier;
		this.listener = listener;
		this.catchThrowables = catchThrowables;

		this.when = System.currentTimeMillis();

		this.promise = new JSPromise<>((resolve, reject) -> {
			this.resolvePromise = resolve;
		});
	}

	public void dispatch() {
		try {
			if (catchThrowables) {
				dispatchCatch();
			} else {
				runnable.run();
			}
		} finally {
			finishDispatch(true);
		}
	}

	private void dispatchCatch() {
		try {
			runnable.run();
		} catch (Throwable t) {
			this.throwable = t;
			if (t instanceof Exception) {
				this.exception = (Exception) t;
			}
		}
	}

	private final void finishDispatch(boolean dispatched) {
		this.dispatched = dispatched;

		resolvePromise.accept(dispatched);

		if (notifier != null) {
			synchronized (notifier) {
				notifier.notifyAll();
			}
		}

		if (listener != null) {
			listener.run();
		}
	}
}

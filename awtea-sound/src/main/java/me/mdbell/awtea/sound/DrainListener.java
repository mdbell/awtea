package me.mdbell.awtea.sound;

public interface DrainListener {

	/**
	 * Called when bytes have been drained from the audio buffer.
	 *
	 * @param bytesDrained The number of bytes that were drained.
	 * @param bytesRemaining The number of bytes remaining in the buffer.
	 * @return true to continue receiving drain notifications, false to unregister.
	 */
	boolean onDrain(int bytesDrained, int bytesRemaining);
}

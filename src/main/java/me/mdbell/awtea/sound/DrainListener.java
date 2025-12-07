package me.mdbell.awtea.sound;

public interface DrainListener {

	/**
	 * Called when frames have been drained from the audio buffer.
	 *
	 * @param framesDrained The number of frames that were drained.
	 * @param framesRemaining The number of frames remaining in the buffer.
	 * @return true to continue receiving drain notifications, false to unregister.
	 */
	boolean onDrain(int framesDrained, int framesRemaining);
}

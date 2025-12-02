package me.mdbell.awtea.classlib.java.awt;


import me.mdbell.awtea.classlib.java.awt.image.TImageObserver;

public class TMediaTracker implements java.io.Serializable {

    /**
     * A given {@code Component} that will be
     * tracked by a media tracker where the image will
     * eventually be drawn.
     *
     * @serial
     * @see #MediaTracker(Component)
     */
    TComponent target;
    /**
     * The head of the list of {@code Images} that is being
     * tracked by the {@code MediaTracker}.
     *
     * @serial
     * @see #addImage(Image, int)
     * @see #removeImage(Image)
     */
    TMediaEntry head;

    /*
     * JDK 1.1 serialVersionUID
     */
    private static final long serialVersionUID = -483174189758638095L;

    /**
     * Creates a media tracker to track images for a given component.
     *
     * @param comp the component on which the images
     *             will eventually be drawn
     */
    public TMediaTracker(TComponent comp) {
        target = comp;
    }

    /**
     * Adds an image to the list of images being tracked by this media
     * tracker. The image will eventually be rendered at its default
     * (unscaled) size.
     *
     * @param image the image to be tracked
     * @param id    an identifier used to track this image
     */
    public void addImage(TImage image, int id) {
        addImage(image, id, -1, -1);
    }

    /**
     * Adds a scaled image to the list of images being tracked
     * by this media tracker. The image will eventually be
     * rendered at the indicated width and height.
     *
     * @param image the image to be tracked
     * @param id    an identifier that can be used to track this image
     * @param w     the width at which the image is rendered
     * @param h     the height at which the image is rendered
     */
    public void addImage(TImage image, int id, int w, int h) {
        addImageImpl(image, id, w, h);
        TImage rvImage = getResolutionVariant(image);
        if (rvImage != null) {
            addImageImpl(rvImage, id,
                    w == -1 ? -1 : 2 * w,
                    h == -1 ? -1 : 2 * h);
        }
    }

    private void addImageImpl(TImage image, int id, int w, int h) {
        head = TMediaEntry.insert(head,
                new TImageTMediaEntry(this, image, id, w, h));
    }

    /**
     * Flag indicating that media is currently being loaded.
     *
     * @see java.awt.MediaTracker#statusAll
     * @see java.awt.MediaTracker#statusID
     */
    public static final int LOADING = 1;

    /**
     * Flag indicating that the downloading of media was aborted.
     *
     * @see java.awt.MediaTracker#statusAll
     * @see java.awt.MediaTracker#statusID
     */
    public static final int ABORTED = 2;

    /**
     * Flag indicating that the downloading of media encountered
     * an error.
     *
     * @see java.awt.MediaTracker#statusAll
     * @see java.awt.MediaTracker#statusID
     */
    public static final int ERRORED = 4;

    /**
     * Flag indicating that the downloading of media was completed
     * successfully.
     *
     * @see java.awt.MediaTracker#statusAll
     * @see java.awt.MediaTracker#statusID
     */
    public static final int COMPLETE = 8;

    static final int DONE = (ABORTED | ERRORED | COMPLETE);

    /**
     * Checks to see if all images being tracked by this media tracker
     * have finished loading.
     * <p>
     * This method does not start loading the images if they are not
     * already loading.
     * <p>
     * If there is an error while loading or scaling an image, then that
     * image is considered to have finished loading. Use the
     * {@code isErrorAny} or {@code isErrorID} methods to
     * check for errors.
     *
     * @return {@code true} if all images have finished loading,
     * have been aborted, or have encountered
     * an error; {@code false} otherwise
     * @see java.awt.MediaTracker#checkAll(boolean)
     * @see java.awt.MediaTracker#checkID
     * @see java.awt.MediaTracker#isErrorAny
     * @see java.awt.MediaTracker#isErrorID
     */
    public boolean checkAll() {
        return checkAll(false, true);
    }

    /**
     * Checks to see if all images being tracked by this media tracker
     * have finished loading.
     * <p>
     * If the value of the {@code load} flag is {@code true},
     * then this method starts loading any images that are not yet
     * being loaded.
     * <p>
     * If there is an error while loading or scaling an image, that
     * image is considered to have finished loading. Use the
     * {@code isErrorAny} and {@code isErrorID} methods to
     * check for errors.
     *
     * @param load if {@code true}, start loading any
     *             images that are not yet being loaded
     * @return {@code true} if all images have finished loading,
     * have been aborted, or have encountered
     * an error; {@code false} otherwise
     * @see java.awt.MediaTracker#checkID
     * @see java.awt.MediaTracker#checkAll()
     * @see java.awt.MediaTracker#isErrorAny()
     * @see java.awt.MediaTracker#isErrorID(int)
     */
    public boolean checkAll(boolean load) {
        return checkAll(load, true);
    }

    private boolean checkAll(boolean load, boolean verify) {
        TMediaEntry cur = head;
        boolean done = true;
        while (cur != null) {
            if ((cur.getStatus(load, verify) & DONE) == 0) {
                done = false;
            }
            cur = cur.next;
        }
        return done;
    }

    /**
     * Checks the error status of all of the images.
     *
     * @return {@code true} if any of the images tracked
     * by this media tracker had an error during
     * loading; {@code false} otherwise
     * @see java.awt.MediaTracker#isErrorID
     * @see java.awt.MediaTracker#getErrorsAny
     */
    public boolean isErrorAny() {
        TMediaEntry cur = head;
        while (cur != null) {
            if ((cur.getStatus(false, true) & ERRORED) != 0) {
                return true;
            }
            cur = cur.next;
        }
        return false;
    }

    /**
     * Returns a list of all media that have encountered an error.
     *
     * @return an array of media objects tracked by this
     * media tracker that have encountered
     * an error, or {@code null} if
     * there are none with errors
     * @see java.awt.MediaTracker#isErrorAny
     * @see java.awt.MediaTracker#getErrorsID
     */
    public Object[] getErrorsAny() {
        TMediaEntry cur = head;
        int numerrors = 0;
        while (cur != null) {
            if ((cur.getStatus(false, true) & ERRORED) != 0) {
                numerrors++;
            }
            cur = cur.next;
        }
        if (numerrors == 0) {
            return null;
        }
        Object errors[] = new Object[numerrors];
        cur = head;
        numerrors = 0;
        while (cur != null) {
            if ((cur.getStatus(false, false) & ERRORED) != 0) {
                errors[numerrors++] = cur.getMedia();
            }
            cur = cur.next;
        }
        return errors;
    }

    /**
     * Starts loading all images tracked by this media tracker. This
     * method waits until all the images being tracked have finished
     * loading.
     * <p>
     * If there is an error while loading or scaling an image, then that
     * image is considered to have finished loading. Use the
     * {@code isErrorAny} or {@code isErrorID} methods to
     * check for errors.
     *
     * @throws InterruptedException if any thread has
     *                              interrupted this thread
     * @see java.awt.MediaTracker#waitForID(int)
     * @see java.awt.MediaTracker#waitForAll(long)
     * @see java.awt.MediaTracker#isErrorAny
     * @see java.awt.MediaTracker#isErrorID
     */
    public void waitForAll() throws InterruptedException {
        waitForAll(0);
    }

    /**
     * Starts loading all images tracked by this media tracker. This
     * method waits until all the images being tracked have finished
     * loading, or until the length of time specified in milliseconds
     * by the {@code ms} argument has passed.
     * <p>
     * If there is an error while loading or scaling an image, then
     * that image is considered to have finished loading. Use the
     * {@code isErrorAny} or {@code isErrorID} methods to
     * check for errors.
     *
     * @param ms the number of milliseconds to wait
     *           for the loading to complete
     * @return {@code true} if all images were successfully
     * loaded; {@code false} otherwise
     * @throws InterruptedException if any thread has
     *                              interrupted this thread.
     * @see java.awt.MediaTracker#waitForID(int)
     * @see java.awt.MediaTracker#waitForAll(long)
     * @see java.awt.MediaTracker#isErrorAny
     * @see java.awt.MediaTracker#isErrorID
     */
    public boolean waitForAll(long ms)
            throws InterruptedException {
        long end = System.currentTimeMillis() + ms;
        boolean first = true;
        while (true) {
            int status = statusAll(first, first);
            if ((status & LOADING) == 0) {
                return (status == COMPLETE);
            }
            first = false;
            long timeout;
            if (ms == 0) {
                timeout = 0;
            } else {
                timeout = end - System.currentTimeMillis();
                if (timeout <= 0) {
                    return false;
                }
            }
            // wait(timeout); // TeaVM: Threading not supported
        }
    }

    /**
     * Calculates and returns the bitwise inclusive <b>OR</b> of the
     * status of all media that are tracked by this media tracker.
     * <p>
     * Possible flags defined by the
     * {@code MediaTracker} class are {@code LOADING},
     * {@code ABORTED}, {@code ERRORED}, and
     * {@code COMPLETE}. An image that hasn't started
     * loading has zero as its status.
     * <p>
     * If the value of {@code load} is {@code true}, then
     * this method starts loading any images that are not yet being loaded.
     *
     * @param load if {@code true}, start loading
     *             any images that are not yet being loaded
     * @return the bitwise inclusive <b>OR</b> of the status of
     * all of the media being tracked
     * @see java.awt.MediaTracker#statusID(int, boolean)
     * @see java.awt.MediaTracker#LOADING
     * @see java.awt.MediaTracker#ABORTED
     * @see java.awt.MediaTracker#ERRORED
     * @see java.awt.MediaTracker#COMPLETE
     */
    public int statusAll(boolean load) {
        return statusAll(load, true);
    }

    private int statusAll(boolean load, boolean verify) {
        TMediaEntry cur = head;
        int status = 0;
        while (cur != null) {
            status = status | cur.getStatus(load, verify);
            cur = cur.next;
        }
        return status;
    }

    /**
     * Checks to see if all images tracked by this media tracker that
     * are tagged with the specified identifier have finished loading.
     * <p>
     * This method does not start loading the images if they are not
     * already loading.
     * <p>
     * If there is an error while loading or scaling an image, then that
     * image is considered to have finished loading. Use the
     * {@code isErrorAny} or {@code isErrorID} methods to
     * check for errors.
     *
     * @param id the identifier of the images to check
     * @return {@code true} if all images have finished loading,
     * have been aborted, or have encountered
     * an error; {@code false} otherwise
     * @see java.awt.MediaTracker#checkID(int, boolean)
     * @see java.awt.MediaTracker#checkAll()
     * @see java.awt.MediaTracker#isErrorAny()
     * @see java.awt.MediaTracker#isErrorID(int)
     */
    public boolean checkID(int id) {
        return checkID(id, false, true);
    }

    /**
     * Checks to see if all images tracked by this media tracker that
     * are tagged with the specified identifier have finished loading.
     * <p>
     * If the value of the {@code load} flag is {@code true},
     * then this method starts loading any images that are not yet
     * being loaded.
     * <p>
     * If there is an error while loading or scaling an image, then that
     * image is considered to have finished loading. Use the
     * {@code isErrorAny} or {@code isErrorID} methods to
     * check for errors.
     *
     * @param id   the identifier of the images to check
     * @param load if {@code true}, start loading any
     *             images that are not yet being loaded
     * @return {@code true} if all images have finished loading,
     * have been aborted, or have encountered
     * an error; {@code false} otherwise
     * @see java.awt.MediaTracker#checkID(int, boolean)
     * @see java.awt.MediaTracker#checkAll()
     * @see java.awt.MediaTracker#isErrorAny()
     * @see java.awt.MediaTracker#isErrorID(int)
     */
    public boolean checkID(int id, boolean load) {
        return checkID(id, load, true);
    }

    private boolean checkID(int id, boolean load, boolean verify) {
        TMediaEntry cur = head;
        boolean done = true;
        while (cur != null) {
            if (cur.getID() == id
                    && (cur.getStatus(load, verify) & DONE) == 0) {
                done = false;
            }
            cur = cur.next;
        }
        return done;
    }

    /**
     * Checks the error status of all of the images tracked by this
     * media tracker with the specified identifier.
     *
     * @param id the identifier of the images to check
     * @return {@code true} if any of the images with the
     * specified identifier had an error during
     * loading; {@code false} otherwise
     * @see java.awt.MediaTracker#isErrorAny
     * @see java.awt.MediaTracker#getErrorsID
     */
    public boolean isErrorID(int id) {
        TMediaEntry cur = head;
        while (cur != null) {
            if (cur.getID() == id
                    && (cur.getStatus(false, true) & ERRORED) != 0) {
                return true;
            }
            cur = cur.next;
        }
        return false;
    }

    /**
     * Returns a list of media with the specified ID that
     * have encountered an error.
     *
     * @param id the identifier of the images to check
     * @return an array of media objects tracked by this media
     * tracker with the specified identifier
     * that have encountered an error, or
     * {@code null} if there are none with errors
     * @see java.awt.MediaTracker#isErrorID
     * @see java.awt.MediaTracker#isErrorAny
     * @see java.awt.MediaTracker#getErrorsAny
     */
    public Object[] getErrorsID(int id) {
        TMediaEntry cur = head;
        int numerrors = 0;
        while (cur != null) {
            if (cur.getID() == id
                    && (cur.getStatus(false, true) & ERRORED) != 0) {
                numerrors++;
            }
            cur = cur.next;
        }
        if (numerrors == 0) {
            return null;
        }
        Object errors[] = new Object[numerrors];
        cur = head;
        numerrors = 0;
        while (cur != null) {
            if (cur.getID() == id
                    && (cur.getStatus(false, false) & ERRORED) != 0) {
                errors[numerrors++] = cur.getMedia();
            }
            cur = cur.next;
        }
        return errors;
    }

    /**
     * Starts loading all images tracked by this media tracker with the
     * specified identifier. This method waits until all the images with
     * the specified identifier have finished loading.
     * <p>
     * If there is an error while loading or scaling an image, then that
     * image is considered to have finished loading. Use the
     * {@code isErrorAny} and {@code isErrorID} methods to
     * check for errors.
     *
     * @param id the identifier of the images to check
     * @throws InterruptedException if any thread has
     *                              interrupted this thread.
     * @see java.awt.MediaTracker#waitForAll
     * @see java.awt.MediaTracker#isErrorAny()
     * @see java.awt.MediaTracker#isErrorID(int)
     */
    public void waitForID(int id) throws InterruptedException {
        waitForID(id, 0);
    }

    /**
     * Starts loading all images tracked by this media tracker with the
     * specified identifier. This method waits until all the images with
     * the specified identifier have finished loading, or until the
     * length of time specified in milliseconds by the {@code ms}
     * argument has passed.
     * <p>
     * If there is an error while loading or scaling an image, then that
     * image is considered to have finished loading. Use the
     * {@code statusID}, {@code isErrorID}, and
     * {@code isErrorAny} methods to check for errors.
     *
     * @param id the identifier of the images to check
     * @param ms the length of time, in milliseconds, to wait
     *           for the loading to complete
     * @return {@code true} if the loading completed in time;
     * otherwise {@code false}
     * @throws InterruptedException if any thread has
     *                              interrupted this thread.
     * @see java.awt.MediaTracker#waitForAll
     * @see java.awt.MediaTracker#waitForID(int)
     * @see java.awt.MediaTracker#statusID
     * @see java.awt.MediaTracker#isErrorAny()
     * @see java.awt.MediaTracker#isErrorID(int)
     */
    public boolean waitForID(int id, long ms)
            throws InterruptedException {
        long end = System.currentTimeMillis() + ms;
        boolean first = true;
        while (true) {
            int status = statusID(id, first, first);
            if ((status & LOADING) == 0) {
                return (status == COMPLETE);
            }
            first = false;
            long timeout;
            if (ms == 0) {
                timeout = 0;
            } else {
                timeout = end - System.currentTimeMillis();
                if (timeout <= 0) {
                    return false;
                }
            }
            // wait(timeout); // TeaVM: Threading not supported
        }
    }

    /**
     * Calculates and returns the bitwise inclusive <b>OR</b> of the
     * status of all media with the specified identifier that are
     * tracked by this media tracker.
     * <p>
     * Possible flags defined by the
     * {@code MediaTracker} class are {@code LOADING},
     * {@code ABORTED}, {@code ERRORED}, and
     * {@code COMPLETE}. An image that hasn't started
     * loading has zero as its status.
     * <p>
     * If the value of {@code load} is {@code true}, then
     * this method starts loading any images that are not yet being loaded.
     *
     * @param id   the identifier of the images to check
     * @param load if {@code true}, start loading
     *             any images that are not yet being loaded
     * @return the bitwise inclusive <b>OR</b> of the status of
     * all of the media with the specified
     * identifier that are being tracked
     * @see java.awt.MediaTracker#statusAll(boolean)
     * @see java.awt.MediaTracker#LOADING
     * @see java.awt.MediaTracker#ABORTED
     * @see java.awt.MediaTracker#ERRORED
     * @see java.awt.MediaTracker#COMPLETE
     */
    public int statusID(int id, boolean load) {
        return statusID(id, load, true);
    }

    private int statusID(int id, boolean load, boolean verify) {
        TMediaEntry cur = head;
        int status = 0;
        while (cur != null) {
            if (cur.getID() == id) {
                status = status | cur.getStatus(load, verify);
            }
            cur = cur.next;
        }
        return status;
    }

    /**
     * Removes the specified image from this media tracker.
     * All instances of the specified image are removed,
     * regardless of scale or ID.
     *
     * @param image the image to be removed
     * @see java.awt.MediaTracker#removeImage(java.awt.Image, int)
     * @see java.awt.MediaTracker#removeImage(java.awt.Image, int, int, int)
     * @since 1.1
     */
    public void removeImage(TImage image) {
        removeImageImpl(image);
        TImage rvImage = getResolutionVariant(image);
        if (rvImage != null) {
            removeImageImpl(rvImage);
        }
        // notifyAll(); // TeaVM: Threading not supported    // Notify in case remaining images are "done".
    }

    private void removeImageImpl(TImage image) {
        TMediaEntry cur = head;
        TMediaEntry prev = null;
        while (cur != null) {
            TMediaEntry next = cur.next;
            if (cur.getMedia() == image) {
                if (prev == null) {
                    head = next;
                } else {
                    prev.next = next;
                }
                cur.cancel();
            } else {
                prev = cur;
            }
            cur = next;
        }
    }

    /**
     * Removes the specified image from the specified tracking
     * ID of this media tracker.
     * All instances of {@code Image} being tracked
     * under the specified ID are removed regardless of scale.
     *
     * @param image the image to be removed
     * @param id    the tracking ID from which to remove the image
     * @see java.awt.MediaTracker#removeImage(java.awt.Image)
     * @see java.awt.MediaTracker#removeImage(java.awt.Image, int, int, int)
     * @since 1.1
     */
    public void removeImage(TImage image, int id) {
        removeImageImpl(image, id);
        TImage rvImage = getResolutionVariant(image);
        if (rvImage != null) {
            removeImageImpl(rvImage, id);
        }
        // notifyAll(); // TeaVM: Threading not supported    // Notify in case remaining images are "done".
    }

    private void removeImageImpl(TImage image, int id) {
        TMediaEntry cur = head;
        TMediaEntry prev = null;
        while (cur != null) {
            TMediaEntry next = cur.next;
            if (cur.getID() == id && cur.getMedia() == image) {
                if (prev == null) {
                    head = next;
                } else {
                    prev.next = next;
                }
                cur.cancel();
            } else {
                prev = cur;
            }
            cur = next;
        }
    }

    /**
     * Removes the specified image with the specified
     * width, height, and ID from this media tracker.
     * Only the specified instance (with any duplicates) is removed.
     *
     * @param image  the image to be removed
     * @param id     the tracking ID from which to remove the image
     * @param width  the width to remove (-1 for unscaled)
     * @param height the height to remove (-1 for unscaled)
     * @see java.awt.MediaTracker#removeImage(java.awt.Image)
     * @see java.awt.MediaTracker#removeImage(java.awt.Image, int)
     * @since 1.1
     */
    public void removeImage(TImage image, int id,
                                         int width, int height) {
        removeImageImpl(image, id, width, height);
        TImage rvImage = getResolutionVariant(image);
        if (rvImage != null) {
            removeImageImpl(rvImage, id,
                    width == -1 ? -1 : 2 * width,
                    height == -1 ? -1 : 2 * height);
        }
        // notifyAll(); // TeaVM: Threading not supported    // Notify in case remaining images are "done".
    }

    private void removeImageImpl(TImage image, int id, int width, int height) {
        TMediaEntry cur = head;
        TMediaEntry prev = null;
        while (cur != null) {
            TMediaEntry next = cur.next;
            if (cur.getID() == id && cur instanceof TImageTMediaEntry
                    && ((TImageTMediaEntry) cur).matches(image, width, height)) {
                if (prev == null) {
                    head = next;
                } else {
                    prev.next = next;
                }
                cur.cancel();
            } else {
                prev = cur;
            }
            cur = next;
        }
    }

    void setDone() {
        // notifyAll(); // TeaVM: Threading not supported
    }

    private static TImage getResolutionVariant(TImage image) {
        return null;
    }
}

abstract class TMediaEntry {
    TMediaTracker tracker;
    int ID;
    TMediaEntry next;

    int status;
    boolean cancelled;

    TMediaEntry(TMediaTracker mt, int id) {
        tracker = mt;
        ID = id;
    }

    abstract Object getMedia();

    static TMediaEntry insert(TMediaEntry head, TMediaEntry me) {
        TMediaEntry cur = head;
        TMediaEntry prev = null;
        while (cur != null) {
            if (cur.ID > me.ID) {
                break;
            }
            prev = cur;
            cur = cur.next;
        }
        me.next = cur;
        if (prev == null) {
            head = me;
        } else {
            prev.next = me;
        }
        return head;
    }

    int getID() {
        return ID;
    }

    abstract void startLoad();

    void cancel() {
        cancelled = true;
    }

    static final int LOADING = java.awt.MediaTracker.LOADING;
    static final int ABORTED = java.awt.MediaTracker.ABORTED;
    static final int ERRORED = java.awt.MediaTracker.ERRORED;
    static final int COMPLETE = java.awt.MediaTracker.COMPLETE;

    static final int LOADSTARTED = (LOADING | ERRORED | COMPLETE);
    static final int DONE = (ABORTED | ERRORED | COMPLETE);

    int getStatus(boolean doLoad, boolean doVerify) {
        if (doLoad && ((status & LOADSTARTED) == 0)) {
            status = (status & ~ABORTED) | LOADING;
            startLoad();
        }
        return status;
    }

    void setStatus(int flag) {
		status = flag;
        tracker.setDone();
    }
}

class TImageTMediaEntry extends TMediaEntry implements TImageObserver,
        java.io.Serializable {
    TImage image;
    int width;
    int height;

    /*
     * JDK 1.1 serialVersionUID
     */
    private static final long serialVersionUID = 4739377000350280650L;

    TImageTMediaEntry(TMediaTracker mt, TImage img, int c, int w, int h) {
        super(mt, c);
        image = img;
        width = w;
        height = h;
    }

    boolean matches(TImage img, int w, int h) {
        return (image == img && width == w && height == h);
    }

    Object getMedia() {
        return image;
    }

    int getStatus(boolean doLoad, boolean doVerify) {
        if (doVerify) {
            int flags = tracker.target.checkImage(image, width, height, null);
            int s = parseflags(flags);
            if (s == 0) {
                if ((status & (ERRORED | COMPLETE)) != 0) {
                    setStatus(ABORTED);
                }
            } else if (s != status) {
                setStatus(s);
            }
        }
        return super.getStatus(doLoad, doVerify);
    }

    void startLoad() {
        if (tracker.target.prepareImage(image, width, height, this)) {
            setStatus(COMPLETE);
        }
    }

    int parseflags(int infoflags) {
        if ((infoflags & ERROR) != 0) {
            return ERRORED;
        } else if ((infoflags & ABORT) != 0) {
            return ABORTED;
        } else if ((infoflags & (ALLBITS | FRAMEBITS)) != 0) {
            return COMPLETE;
        }
        return 0;
    }

    public boolean imageUpdate(TImage img, int infoflags,
                               int x, int y, int w, int h) {
        if (cancelled) {
            return false;
        }
        int s = parseflags(infoflags);
        if (s != 0 && s != status) {
            setStatus(s);
        }
        return ((status & LOADING) != 0);
    }
}

package org.rocstreaming.roctoolkit;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * A <code>NativeObject</code> represents an underlying native roc object.
 */
class NativeObject implements AutoCloseable {

    /**
     * Maximum time to wait (in milliseconds) for joining {@link AutoCloseThread}.
     */
    private final static long MAX_JOIN_TIMEOUT_MS = SECONDS.toMillis(20L);

    /**
     * <code>NativeObject</code> finalizer thread.
     */
    private final static AutoCloseThread thread = AutoCloseThread.getInstance();

    /**
     *  Underlying roc object native pointer.
     */
    private final long ptr;

    /**
     *  Destructor method.
     */
    private final Destructor destructor;

    /**
     *  Reference to {@link NativeObjectReference}.
     */
    private final NativeObjectReference resource;

    static {
        RocLibrary.loadLibrary();
        thread.start();

        /* add a ShutdownHook for closing all NativeObjects that are still open */
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            thread.closeAll();
            try {
                thread.join(MAX_JOIN_TIMEOUT_MS);
            } catch (InterruptedException e) {}
        }));
    }

    /**
     * Construct a <code>NativeObject</code>.
     *
     * @param ptr               native pointer to a roc native object
     * @param dependsOn         <code>NativeObject</code> dependency for
     *                          finalization ordering
     * @param destructor        destructor method for closing <code>NativeObject</code>.
     */
    protected NativeObject(long ptr, NativeObject dependsOn, Destructor destructor) {
        this.ptr = ptr;
        this.destructor = destructor;
        this.resource = thread.add(this, dependsOn);
    }

    /**
     * Construct a <code>NativeObject</code>.
     *
     * @param ptr               native pointer to a roc native object.
     * @param destructor        destructor method for closing <code>NativeObject</code>.
     */
    protected NativeObject(long ptr, Destructor destructor) {
        this(ptr, null, destructor);
    }

    /**
     * Get <code>NativeObject</code> native pointer.
     *
     * @return                  the native roc object pointer associated to this
     *                          <code>NativeObject</code>.
     */
    long getPtr() {
        return this.ptr;
    }

    /**
     * Get <code>NativeObject</code> destructor.
     *
     * @return                  destructor method for closing <code>NativeObject</code>.
     */
    Destructor getDestructor() {
        return this.destructor;
    }

    /**
     * Close the native object and remove it from the {@link AutoCloseThread}.
     *
     * @throws Exception        if the underlying roc native object cannot be closed.
     */
    @Override
    public void close() throws Exception {
        thread.remove(resource);
        resource.close();
    }
}

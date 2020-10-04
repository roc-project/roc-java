package org.rocstreaming.roctoolkit;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <code>NativeObjectReference</code> is associated with a {@link NativeObject} and owns its entire lifetime;
 *
 * A <code>NativeObjectReference</code> contains necessary data for closing the native object
 * after it becomes phantom reachable.
 */
class NativeObjectReference extends PhantomReference<NativeObject> implements AutoCloseable {

    /**
     *  Underlying roc object native pointer.
     */
    private final long ptr;

    /**
     *  Destructor method
     */
    private final Destructor destructor;

    /**
     *  Avoid {@link NativeObject} dependency to be garbage collected unless this
     * <code>NativeObjectReference</code> is closed first (for example for avoiding
     * {@link Context} to be closed before closing {@link Sender} or {@link Receiver}).
     */
    private final AtomicReference<NativeObject> dependsOn;

    /**
     *  {@link NativeObject} open status.
     */
    private final AtomicBoolean isOpen;

    /**
     * Construct a new <code>NativeObjectReference</code>.
     *
     * @param referent          {@link NativeObject} associated.
     * @param dependsOn         {@link NativeObject} dependency for finalization ordering.
     * @param queue             Reference queue containing phantom reachable native objects.
     */
    NativeObjectReference(NativeObject referent, NativeObject dependsOn,
                        ReferenceQueue<? super NativeObject> queue) {
        super(referent, queue);
        this.ptr = referent.getPtr();
        this.destructor = referent.getDestructor();
        this.dependsOn = new AtomicReference<>(dependsOn);
        this.isOpen = new AtomicBoolean(true);
    }

    /**
     * Get {@link NativeObject} native pointer.
     *
     * @return      the native roc object pointer associated to this <code>NativeObjectReference</code>.
     */
    long getPtr() {
        return ptr;
    }

    /**
     * Get {@link NativeObject} destructor.
     *
     * @return      destructor method for closing {@link NativeObject}.
     */
    Destructor getDestructor() {
        return destructor;
    }

    /**
     * Get {@link NativeObject} dependency for finalization ordering.
     *
     * @return      an <code>Optional</code> describing the {@link NativeObject} dependency.
     */
    Optional<NativeObject> getDependsOn() {
        return Optional.ofNullable(dependsOn.get());
    }

    /**
     * Get {@link NativeObject} open status.
     *
     * @return true is the {@link NativeObject} is still open,
     *         false otherwise.
     */
    boolean isOpen() {
        return isOpen.get();
    }

    /**
     * Close the native object.
     */
    @Override
    public void close() throws Exception {
        if (isOpen()) {
            this.isOpen.set(false);
            destructor.close(ptr);
            this.dependsOn.set(null);
        }
    }
}

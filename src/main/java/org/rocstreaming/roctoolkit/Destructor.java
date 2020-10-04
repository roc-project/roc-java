package org.rocstreaming.roctoolkit;

/**
 *  Interface providing a method for destruct a {@link NativeObject}.
 */
@FunctionalInterface
interface Destructor {

    /**
     * Close {@link NativeObject}.
     *
     * This method can be called synchronously by the
     * user or asynchronously by {@link AutoCloseThread}.
     *
     * <p style="color: red;">
     * Note: It's important that this method is declared <code>static</code> and not as an
     * instance method for avoiding object resurrection.
     * </p>
     *
     * @param resource      {@link NativeObject#ptr NativeObject.ptr} to be closed.
     *
     * @throws Exception    if the {@link NativeObject} cannot be closed (for example
     *                      for still opened {@link NativeObject} dependencies).
     */
    void close(long resource) throws Exception;
}

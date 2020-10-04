package org.rocstreaming.roctoolkit;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collect open {@link NativeObjectReference} for avoid being garbage collected.
 *
 * @param <T>       the type of {@link NativeObjectReference} to be collected.
 */
class ReferenceCollector<T extends NativeObjectReference> implements Iterable<T> {

    /**
     * References map indexed by their native pointers.
     */
    private final Map<Long, T> references;

    /**
     * Create a new <code>ReferenceCollector</code>.
     */
    ReferenceCollector() {
        references = new ConcurrentHashMap<>();
    }

    /**
     * Add a new reference to the collection.
     *
     * @param reference         reference to add.
     */
    void add(final T reference) {
        references.put(reference.getPtr(), reference);
    }

    /**
     * Remove a reference from the collection.
     *
     * @param reference         reference to remove.
     */
    void remove(final T reference) {
        references.remove(reference.getPtr());
    }

    /**
     * Get the number of references inside the collection.
     *
     * @return      number of references.
     */
    int size() {
        return references.size();
    }

    /**
     * Iterator over <code>ReferenceCollector</code> iterating references with dependencies
     * ({@link NativeObjectReference#dependsOn}) firstly and secondly the references without
     * dependencies.
     */
    class ReferenceCollectorIterator implements Iterator<T> {

        /**
         * Iterator over references with dependencies.
         */
        private Iterator<T> withDependencies;

        /**
         * Iterator over references without dependencies.
         */
        private Iterator<T> withoutDependencies;

        /**
         * Create a new <code>ReferenceCollectorIterator</code>.
         */
        ReferenceCollectorIterator() {
            withDependencies = references.values().parallelStream()
                            .filter(r -> r.getDependsOn().isPresent())
                            .iterator();
            withoutDependencies = references.values().parallelStream()
                            .filter(r -> !r.getDependsOn().isPresent())
                            .iterator();
        }

        @Override
        public boolean hasNext() {
            return withDependencies.hasNext() || withoutDependencies.hasNext();
        }

        @Override
        public T next() {
            if (withDependencies.hasNext()) return withDependencies.next();
            if (withoutDependencies.hasNext()) return withoutDependencies.next();
            return null;
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new ReferenceCollectorIterator();
    }
}

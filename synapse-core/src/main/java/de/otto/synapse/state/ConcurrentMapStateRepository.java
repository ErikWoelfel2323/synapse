package de.otto.synapse.state;

import org.slf4j.Logger;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * High performance in-memory StateRepository implemented using a ConcurrentMap.
 *
 * @param <V>
 */
public class ConcurrentMapStateRepository<V> implements StateRepository<V> {

    private static final Logger LOG = getLogger(ConcurrentMapStateRepository.class);

    private final String name;
    private final ConcurrentMap<String, V> concurrentMap;

    /**
     * Creates a StateRepository with the given name, that is using a {@code ConcurrentHashMap} to store
     * event-sourced entities.
     *
     * @param name the {@link #getName() name}  of the repository.
     */
    public ConcurrentMapStateRepository(final String name) {
        this(name, new ConcurrentHashMap<>());
    }

    /**
     * Creates a StateRepository with the given name, that is using the given {@code ConcurrentMap} to store
     * event-sourced entities.
     *
     * <p>Usable to create StateRepository instances from ConcurrentMap implementations like, for example,
     * ChronicleMap</p>
     *
     * @param name the {@link #getName() name}  of the repository.
     * @param map the delegate map used to store the entity state
     */
    public ConcurrentMapStateRepository(final String name,
                                        final ConcurrentMap<String, V> map) {
        this.name = requireNonNull(name, "Parameter 'name' must not be null");
        this.concurrentMap = requireNonNull(map, "Parameter 'map' must not be null");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Optional<V> compute(final String key, final BiFunction<? super String, ? super Optional<V>, ? extends V> remappingFunction) {
        return ofNullable(concurrentMap.compute(key, (k, v) -> remappingFunction.apply(k, ofNullable(v))));
    }

    @Override
    public void consumeAll(BiConsumer<? super String, ? super V> consumer) {
        concurrentMap.forEach(consumer);
    }

    @Override
    public Optional<V> put(final String key, final V value) {
        return ofNullable(concurrentMap.put(key, value));
    }

    @Override
    public Optional<V> remove(final String key) {
        return ofNullable(concurrentMap.remove(key));
    }

    @Override
    public void clear() {
        concurrentMap.clear();
    }

    @Override
    public Optional<V> get(final String key) {
        return ofNullable(concurrentMap.get(key));
    }

    @Override
    public Set<String> keySet() {
        return unmodifiableSet(concurrentMap.keySet());
    }

    @Override
    public long size() {
        return concurrentMap.size();
    }

    @Override
    public void close() throws Exception {
        LOG.info("Closing StateRepository.");
        if (concurrentMap instanceof AutoCloseable) {
            ((AutoCloseable) concurrentMap).close();
        }
    }
}

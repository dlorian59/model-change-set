package io.github.alex59.modelchanges;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Compares explicit model projections by stable identity. A differ is immutable;
 * sharing it is safe when its supplied functions and input models are safe to share.
 * Keys must have stable equality and a total comparator consistent with equality.
 * No reflection, recursive graph traversal, or implicit field discovery is used.
 *
 * @param <T> model type; null model entries are rejected
 * @param <K> stable key type; null keys are rejected
 */
public final class ModelDiffer<T, K> {
    private final Function<? super T, ? extends K> key;
    private final Comparator<? super K> order;
    private final List<Field<T, ?>> fields;

    private ModelDiffer(Function<? super T, ? extends K> key, Comparator<? super K> order,
                        List<Field<T, ?>> fields) {
        this.key = Objects.requireNonNull(key, "key");
        this.order = Objects.requireNonNull(order, "order");
        this.fields = List.copyOf(fields);
        var names = new HashSet<String>();
        for (Field<T, ?> field : this.fields) {
            if (!names.add(field.name())) {
                throw new IllegalArgumentException("Duplicate field name: " + field.name());
            }
        }
    }

    /**
     * Defines a differ with an explicit key order. An empty field list compares
     * only membership. Duplicate field names are rejected.
     *
     * @param key stable key projection
     * @param order total key order, consistent with equals
     * @param fields projections in report order
     * @param <T> model type
     * @param <K> key type
     * @return immutable differ
     */
    public static <T, K> ModelDiffer<T, K> of(Function<? super T, ? extends K> key,
            Comparator<? super K> order, List<Field<T, ?>> fields) {
        return new ModelDiffer<>(key, order, fields);
    }

    /**
     * Starts a definition using the key's natural order. No fields are discovered
     * implicitly; a builder with no fields compares membership only.
     * <p>Начинает настройку с естественным порядком ключей. Поля выбираются явно;
     * без полей сравнивается только состав.</p>
     * @param key stable, non-null key projection
     * @param <T> model type
     * @param <K> comparable key type; natural order must be consistent with equals
     * @return mutable definition builder, intended for single-threaded setup
     */
    public static <T, K extends Comparable<? super K>> Builder<T, K> keyedBy(
            Function<? super T, ? extends K> key) {
        return keyedBy(key, Comparator.naturalOrder());
    }

    /**
     * Starts a definition with an explicit key order.
     * <p>Начинает настройку с явно заданным порядком ключей.</p>
     * @param key stable, non-null key projection
     * @param order total key order consistent with equals
     * @param <T> model type
     * @param <K> key type
     * @return mutable definition builder, intended for single-threaded setup
     */
    public static <T, K> Builder<T, K> keyedBy(Function<? super T, ? extends K> key,
                                             Comparator<? super K> order) {
        return new Builder<>(key, order);
    }

    /**
     * Collects explicitly selected business fields in report order. The builder
     * is not thread-safe; each build snapshots its definition into an immutable
     * differ. Later additions do not alter previously built differs.
     * <p>Собирает бизнес-поля в порядке отчёта. Построитель не потокобезопасен;
     * build фиксирует независимый неизменяемый набор настроек.</p>
     * @param <T> model type
     * @param <K> stable key type
     */
    public static final class Builder<T, K> {
        private final Function<? super T, ? extends K> key;
        private final Comparator<? super K> order;
        private final List<Field<T, ?>> fields = new ArrayList<>();

        private Builder(Function<? super T, ? extends K> key, Comparator<? super K> order) {
            this.key = Objects.requireNonNull(key, "key");
            this.order = Objects.requireNonNull(order, "order");
        }

        /**
         * Selects a field with the default null-safe deep equality policy.
         * <p>Выбирает поле с обычным глубоким равенством и обработкой null.</p>
         * @param name nonblank name, unique within this definition
         * @param extractor value projection
         * @param <V> field type
         * @return this builder
         */
        public <V> Builder<T, K> field(String name, Function<? super T, ? extends V> extractor) {
            fields.add(Field.of(name, extractor));
            return this;
        }

        /**
         * Selects a field with explicit equivalence. Custom policies receive nulls
         * unchanged; {@link FieldEquivalence} provides common null-safe choices.
         * <p>Выбирает поле и правило равенства. Null передаётся правилу без изменений;
         * готовые правила FieldEquivalence умеют обрабатывать null.</p>
         * @param name nonblank name, unique within this definition
         * @param extractor value projection
         * @param equivalent true when values should be treated as equal
         * @param <V> field type
         * @return this builder
         */
        public <V> Builder<T, K> field(String name, Function<? super T, ? extends V> extractor,
                                      BiPredicate<? super V, ? super V> equivalent) {
            fields.add(Field.comparing(name, extractor, equivalent));
            return this;
        }

        /**
         * Creates an immutable definition using the existing comparison engine.
         * <p>Создаёт неизменяемое определение для прежнего алгоритма сравнения.</p>
         * @return differ; an empty field selection compares only membership
         * @throws IllegalArgumentException if field names are duplicated
         */
        public ModelDiffer<T, K> build() {
            return ModelDiffer.of(key, order, fields);
        }
    }

    /**
     * Compares finite snapshots in any input order. Each input is consumed once
     * and fully indexed before comparing fields. Duplicate keys in either input
     * are rejected, even when the duplicate models are equal.
     *
     * @param before old snapshot
     * @param after new snapshot
     * @return changes sorted by key; field differences follow definition order
     * @throws IllegalArgumentException if a snapshot contains duplicate keys or
     *         comparator-equivalent but unequal keys
     * @throws NullPointerException if an input, model, or key is null
     */
    public ChangeSet<T, K> compare(Iterable<? extends T> before, Iterable<? extends T> after) {
        var oldModels = index(before, "before");
        var newModels = index(after, "after");
        var changes = new ArrayList<Change<T, K>>();
        merge(oldModels.entrySet().iterator(), newModels.entrySet().iterator(), changes::add);
        return new ChangeSet<>(changes);
    }

    /**
     * Emits changes while merge-reading two strictly sorted snapshots. Retains
     * one current entry per input plus one change, independent of snapshot size.
     * Each key projection is evaluated once. Iterators and consumer are owned by
     * the caller and are not closed by this method.
     *
     * <p>Validation is incremental: an invalid later entry or a failing callback
     * can leave an already emitted prefix. Use {@link #compare(Iterable, Iterable)}
     * when a caller-visible all-or-nothing result is required. This method does
     * not make consumer side effects transactional.</p>
     *
     * @param before old snapshot in strictly increasing key order
     * @param after new snapshot in strictly increasing key order
     * @param consumer receives each change synchronously in key order
     * @throws IllegalArgumentException for duplicates, descending keys, or key collisions
     * @throws NullPointerException for null arguments, models, or keys
     */
    public void compareSorted(Iterator<? extends T> before, Iterator<? extends T> after,
                              Consumer<? super Change<T, K>> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        merge(keyed(Objects.requireNonNull(before, "before"), "before"),
                keyed(Objects.requireNonNull(after, "after"), "after"), consumer);
    }

    private TreeMap<K, T> index(Iterable<? extends T> input, String side) {
        Objects.requireNonNull(input, side);
        var result = new TreeMap<K, T>(order);
        for (T model : input) {
            K identity = identity(model, side);
            var existing = result.ceilingEntry(identity);
            if (existing != null && order.compare(existing.getKey(), identity) == 0) {
                requireSameKey(existing.getKey(), identity);
                throw new IllegalArgumentException("Duplicate key in " + side + ": " + identity);
            }
            result.put(identity, model);
        }
        return result;
    }

    private K identity(T model, String side) {
        Objects.requireNonNull(model, "Null model in " + side);
        return Objects.requireNonNull(key.apply(model), "Null key in " + side);
    }

    private Iterator<Map.Entry<K, T>> keyed(Iterator<? extends T> source, String side) {
        return new Iterator<>() {
            @Override public boolean hasNext() { return source.hasNext(); }
            @Override public Map.Entry<K, T> next() {
                T model = source.next();
                return new AbstractMap.SimpleImmutableEntry<>(identity(model, side), model);
            }
        };
    }

    private void requireSameKey(K first, K second) {
        if (!Objects.equals(first, second)) {
            throw new IllegalArgumentException("Comparator treats unequal keys as identical: "
                    + first + " and " + second);
        }
    }

    private void merge(Iterator<Map.Entry<K, T>> before, Iterator<Map.Entry<K, T>> after,
                       Consumer<? super Change<T, K>> consumer) {
        var oldModels = new Cursor(before, "before");
        var newModels = new Cursor(after, "after");
        while (oldModels.current != null || newModels.current != null) {
            int comparison = oldModels.current == null ? 1 : newModels.current == null ? -1
                    : order.compare(oldModels.current.getKey(), newModels.current.getKey());
            if (comparison < 0) {
                var old = oldModels.current;
                consumer.accept(new Change<>(old.getKey(), ChangeType.REMOVED, old.getValue(), null, List.of()));
                oldModels.advance();
            } else if (comparison > 0) {
                var next = newModels.current;
                consumer.accept(new Change<>(next.getKey(), ChangeType.ADDED, null, next.getValue(), List.of()));
                newModels.advance();
            } else {
                var old = oldModels.current;
                var next = newModels.current;
                requireSameKey(old.getKey(), next.getKey());
                var differences = new ArrayList<FieldChange>();
                for (Field<T, ?> field : fields) {
                    try {
                        field.compare(old.getValue(), next.getValue()).ifPresent(differences::add);
                    } catch (RuntimeException failure) {
                        throw new IllegalStateException("Cannot compare field '" + field.name()
                                + "' at key " + old.getKey(), failure);
                    }
                }
                if (!differences.isEmpty()) {
                    consumer.accept(new Change<>(old.getKey(), ChangeType.MODIFIED,
                            old.getValue(), next.getValue(), differences));
                }
                oldModels.advance();
                newModels.advance();
            }
        }
    }

    private final class Cursor {
        private final Iterator<Map.Entry<K, T>> source;
        private final String side;
        private Map.Entry<K, T> current;

        private Cursor(Iterator<Map.Entry<K, T>> source, String side) {
            this.source = source;
            this.side = side;
            advance();
        }

        private void advance() {
            var previous = current;
            current = source.hasNext() ? source.next() : null;
            if (previous != null && current != null) {
                int comparison = order.compare(previous.getKey(), current.getKey());
                if (comparison == 0) {
                    requireSameKey(previous.getKey(), current.getKey());
                    throw new IllegalArgumentException("Duplicate key in " + side + ": " + current.getKey());
                }
                if (comparison > 0) {
                    throw new IllegalArgumentException("Keys in " + side + " are not strictly increasing: "
                            + previous.getKey() + " then " + current.getKey());
                }
            }
        }
    }
}

package io.github.alex59.modelchanges;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ModelDifferBuilderTest {
    record Product(String sku, String name, BigDecimal price) { }

    @Test void builderPreservesSnapshotSortedAndCustomOrderSemantics() {
        var builder = ModelDiffer.keyedBy(Product::sku)
                .field("price", Product::price, FieldEquivalence.decimalValue());
        var priceOnly = builder.build();
        var both = builder.field("name", Product::name).build();
        var before = List.of(new Product("a", "old", new BigDecimal("1.0")),
                new Product("c", "removed", null));
        var after = List.of(new Product("a", "new", new BigDecimal("1.00")),
                new Product("b", "added", null));
        assertTrue(priceOnly.compare(before, after).modified().isEmpty());
        assertEquals(List.of("name"), both.compare(before, after).modified().get(0)
                .fields().stream().map(FieldChange::name).toList());
        var streamed = new ArrayList<Change<Product, String>>();
        both.compareSorted(before.iterator(), after.iterator(), streamed::add);
        assertEquals(both.compare(before, after).changes(), streamed);
        var reverse = ModelDiffer.keyedBy(Product::sku, Comparator.reverseOrder()).build();
        assertEquals(List.of("c", "b"), reverse.compare(before, after).changes().stream().map(Change::key).toList());
        assertThrows(IllegalArgumentException.class, () -> builder.field("name", Product::name).build());
        assertThrows(IllegalArgumentException.class, () -> priceOnly.compare(List.of(before.get(0), before.get(0)), after));
        var colliding = ModelDiffer.keyedBy(Product::sku, String.CASE_INSENSITIVE_ORDER).build();
        assertThrows(IllegalArgumentException.class, () -> colliding.compare(List.of(before.get(0)),
                List.of(new Product("A", "same comparator position", null))));
    }

    @Test void namedPoliciesKeepNullTransitionsAndOriginalChangedValues() {
        var differ = ModelDiffer.keyedBy(Product::sku)
                .field("price", Product::price, FieldEquivalence.decimalValue())
                .field("name", Product::name, FieldEquivalence.ignoringCase()).build();
        assertTrue(differ.compare(List.of(new Product("a", "Mug", new BigDecimal("1.0"))),
                List.of(new Product("a", "MUG", new BigDecimal("1.00")))).isEmpty());
        var changed = differ.compare(List.of(new Product("a", null, null)),
                List.of(new Product("a", "", new BigDecimal("1.00"))));
        assertEquals(List.of(new FieldChange("price", null, new BigDecimal("1.00")),
                new FieldChange("name", null, "")), changed.modified().get(0).fields());
        assertTrue(differ.compare(List.of(new Product("a", null, null)),
                List.of(new Product("a", null, null))).isEmpty());
        assertFalse(differ.compare(List.of(new Product("a", "Mug", new BigDecimal("1.00"))),
                List.of(new Product("a", " Mug", new BigDecimal("1.01")))).isEmpty());
    }

    @Test void nullSafeAdapterCallsDomainPolicyOnlyForTwoValuesWithoutChangingLegacyPolicy() {
        var calls = new AtomicInteger();
        var custom = FieldEquivalence.nullSafe((String left, String right) -> {
            calls.incrementAndGet();
            return left.strip().equals(right.strip());
        });
        assertTrue(custom.test(null, null));
        assertFalse(custom.test(null, ""));
        assertFalse(custom.test("", null));
        assertEquals(0, calls.get());
        assertTrue(custom.test("stock ", "stock"));
        assertEquals(1, calls.get());
        var legacy = ModelDiffer.keyedBy(Product::sku)
                .field("name", Product::name, (left, right) -> left == null || right == null).build();
        assertTrue(legacy.compare(List.of(new Product("a", null, null)),
                List.of(new Product("a", "domain treats null as wildcard", null))).isEmpty());
        assertThrows(NullPointerException.class, () -> FieldEquivalence.nullSafe(null));
    }
}

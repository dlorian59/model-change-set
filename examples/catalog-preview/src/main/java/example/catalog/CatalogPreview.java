package example.catalog;

import io.github.alex59.modelchanges.Change;
import io.github.alex59.modelchanges.ChangeType;
import io.github.alex59.modelchanges.FieldChange;
import io.github.alex59.modelchanges.FieldEquivalence;
import io.github.alex59.modelchanges.ModelDiffer;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CatalogPreview {
    private CatalogPreview() { }

    public static void main(String[] args) {
        var yesterday = Instant.parse("2026-11-04T12:00:00Z");
        var today = yesterday.plusSeconds(86400);
        var current = List.of(
                new Product("P-3", "Old mug", new BigDecimal("6.00"), yesterday),
                new Product("P-1", "Notebook", new BigDecimal("10.0"), yesterday),
                new Product("P-2", "Pencil", new BigDecimal("2.00"), yesterday));
        var incoming = List.of(
                new Product("P-4", "Folder", new BigDecimal("4.00"), today),
                new Product("P-2", "Pencil", new BigDecimal("2.50"), today),
                new Product("P-1", "Notebook", new BigDecimal("10.00"), today));

        // A real service call: unordered snapshots become a stable approval report.
        // Настоящий вызов сервиса: неупорядоченные снимки превращаются в стабильный отчёт.
        var result = new ProductCatalogService().preview(current, incoming);
        require(result.changes().stream().map(Change::key).toList().equals(List.of("P-2", "P-3", "P-4")),
                "Changes must be sorted by SKU");
        require(result.changes().stream().map(Change::type).toList().equals(
                List.of(ChangeType.MODIFIED, ChangeType.REMOVED, ChangeType.ADDED)), "Change direction");
        require(result.modified().get(0).fields().equals(
                List.of(new FieldChange("price", new BigDecimal("2.00"), new BigDecimal("2.50")))),
                "Only price changed; decimal scale and importedAt are ignored");
        require(result.added().size() == 1 && result.removed().size() == 1, "Membership report");
        result.changes().forEach(change -> System.out.println(change.type() + " " + change.key()
                + " " + change.fields().stream().map(FieldChange::name).toList()));

        // Omit fields when only additions/removals matter; there is no implicit reflection.
        // Не задаём поля, когда нужен только состав; автоматического обхода через reflection нет.
        var membership = ModelDiffer.keyedBy(Product::sku).build();
        var membershipReport = membership.compare(current, incoming);
        require(membershipReport.modified().isEmpty() && membershipReport.changes().size() == 2,
                "Membership ignores changed prices");

        // Choose case-insensitive text explicitly; it does not change the original values.
        // Явно выбираем сравнение без учёта регистра; исходные значения не меняются.
        var names = ModelDiffer.keyedBy(Product::sku)
                .field("name", Product::name, FieldEquivalence.ignoringCase()).build();
        require(names.compare(List.of(new Product("P-1", "Notebook", null, yesterday)),
                List.of(new Product("P-1", "NOTEBOOK", null, today))).isEmpty(), "Text equivalence");

        // Adapt a business-specific comparison without writing repetitive null checks.
        // Подключаем своё правило без повторяющихся проверок на null.
        var foldedNames = ModelDiffer.keyedBy(Product::sku)
                .field("name", Product::name, FieldEquivalence.nullSafe(
                        (String left, String right) -> left.strip().equals(right.strip())))
                .build();
        require(foldedNames.compare(List.of(new Product("P-1", "Notebook ", null, yesterday)),
                List.of(new Product("P-1", "Notebook", null, today))).isEmpty(), "Custom equivalence");

        // Sorted mode consumes already sorted sources; the application owns their lifecycle.
        // Последовательный режим читает уже отсортированные источники; их закрывает приложение.
        var prices = ModelDiffer.keyedBy(Product::sku)
                .field("price", Product::price, FieldEquivalence.decimalValue()).build();
        var bySku = Comparator.comparing(Product::sku);
        var emitted = new ArrayList<Change<Product, String>>();
        prices.compareSorted(current.stream().sorted(bySku).iterator(),
                incoming.stream().sorted(bySku).iterator(), emitted::add);
        require(emitted.equals(result.changes()), "Sorted and snapshot modes must agree");
        require(new ProductCatalogService().preview(incoming, incoming).isEmpty(), "Converged catalog");
        require(current.get(2).price().equals(new BigDecimal("2.00")), "Preview must not mutate input");
        System.out.println("added=1; removed=1; modified=1; membership=2; sortedMatches=true");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

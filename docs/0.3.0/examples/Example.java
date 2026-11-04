import io.github.alex59.modelchanges.Change;
import io.github.alex59.modelchanges.ChangeSet;
import io.github.alex59.modelchanges.ChangeType;
import io.github.alex59.modelchanges.Field;
import io.github.alex59.modelchanges.ModelDiffer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// Run every documented scenario against the core library on Java 17+.
// Выполняем все сценарии руководства с основной библиотекой на Java 17+.
public final class Example {
    private Example() {}

    record Column(String key, String type, boolean nullable) {}

    public static void main(String[] args) {
        // Choose identity and the business fields included in a schema review.
        // Выбираем ключ и значимые поля для проверки изменений схемы.
        List<Field<Column, ?>> fields = List.of(
            Field.of("type", Column::type),
            Field.of("nullable", Column::nullable)
        );
        var differ = ModelDiffer.<Column, String>of(
            Column::key, Comparator.naturalOrder(), fields
        );
        var before = List.of(
            new Column("orders.note", "text", true),
            new Column("orders.id", "bigint", false),
            new Column("orders.legacy", "text", true)
        );
        var after = List.of(
            new Column("orders.id", "bigint", false),
            new Column("orders.created_at", "timestamp", false),
            new Column("orders.note", "text", false)
        );
        var changes = differ.compare(before, after);
        changes.changes().forEach(change -> {
            System.out.println(change.type() + " " + change.key());
            change.fields().forEach(field -> System.out.println(
                "  " + field.name() + ": " + field.before() + " -> " + field.after()
            ));
        });

        // Input order is irrelevant; the comparator defines report order.
        // Порядок входа не важен; порядок отчёта задаёт компаратор.
        var reorderedBefore = new ArrayList<>(before);
        Collections.reverse(reorderedBefore);
        System.out.println("same report=" + changes.equals(
            differ.compare(reorderedBefore, after)
        ));
        var reverseDiffer = ModelDiffer.<Column, String>of(
            Column::key, Comparator.reverseOrder(), fields
        );
        System.out.println("descending=" + reverseDiffer.compare(before, after)
            .changes().stream().map(Change::key).toList());

        // A price policy ignores decimal scale but explicitly handles null.
        // Правило цены игнорирует масштаб десятичного числа и явно обрабатывает null.
        record Price(String key, BigDecimal amount) {}
        var amount = Field.<Price, BigDecimal>comparing(
            "amount", Price::amount,
            (a, b) -> a == null ? b == null
                : b != null && a.compareTo(b) == 0
        );
        var priceDiffer = ModelDiffer.<Price, String>of(
            Price::key, Comparator.naturalOrder(), List.of(amount)
        );
        System.out.println("field=" + amount.name());
        System.out.println("same amount=" + priceDiffer.compare(
            List.of(new Price("price", new BigDecimal("1.0"))),
            List.of(new Price("price", new BigDecimal("1.00")))
        ).isEmpty());
        var priceChange = priceDiffer.compare(
            List.of(new Price("price", null)),
            List.of(new Price("price", new BigDecimal("1.00")))
        ).modified().get(0).fields().get(0);
        System.out.println(priceChange.name() + ": "
            + priceChange.before() + " -> " + priceChange.after());

        // A missing model and a present model with a null field are different events.
        // Отсутствующая модель и модель с пустым полем null — разные события.
        var nullToEmpty = differ.compare(
            List.of(new Column("note", null, true)),
            List.of(new Column("note", "", true))
        );
        var absentToNull = differ.compare(
            List.of(), List.of(new Column("note", null, true))
        );
        System.out.println("null-to-empty modified=" + nullToEmpty.modified().size());
        System.out.println("absent-to-null added=" + absentToNull.added().size());

        // Default deep equality compares array contents without custom adapters.
        // Стандартное глубокое равенство сравнивает содержимое массивов без адаптеров.
        record Blob(String key, byte[] bytes) {}
        var blobDiffer = ModelDiffer.<Blob, String>of(
            Blob::key, Comparator.naturalOrder(), List.of(Field.of("bytes", Blob::bytes))
        );
        System.out.println("equal arrays=" + blobDiffer.compare(
            List.of(new Blob("blob", new byte[] {1, 2})),
            List.of(new Blob("blob", new byte[] {1, 2}))
        ).isEmpty());

        // Filter an immutable report into additions, removals and modifications.
        // Разделяем неизменяемый отчёт на добавления, удаления и модификации.
        System.out.println("added=" + changes.added().stream().map(Change::key).toList());
        System.out.println("removed=" + changes.removed().stream().map(Change::key).toList());
        System.out.println("modified=" + changes.modified().stream().map(Change::key).toList());
        System.out.println("has changes=" + !changes.isEmpty());

        // An empty projection compares membership; a subset ignores unrelated fields.
        // Пустой набор полей сравнивает состав; подмножество игнорирует остальные поля.
        var membershipOnly = ModelDiffer.<Column, String>of(
            Column::key, Comparator.naturalOrder(), List.of()
        );
        System.out.println("membership changes=" + membershipOnly.compare(before, after)
            .changes().size());
        System.out.println("same identity=" + membershipOnly.compare(
            List.of(new Column("id", "int", true)),
            List.of(new Column("id", "bigint", false))
        ).isEmpty());

        var typeOnly = ModelDiffer.<Column, String>of(
            Column::key, Comparator.naturalOrder(), List.of(Field.of("type", Column::type))
        );
        System.out.println("ignored nullable=" + typeOnly.compare(
            List.of(new Column("id", "int", true)),
            List.of(new Column("id", "int", false))
        ).isEmpty());

        // Sorted inputs can emit changes incrementally without indexing both snapshots.
        // Отсортированные входы выдают изменения последовательно без индексации снимков.
        var emitted = new ArrayList<Change<Column, String>>();
        differ.compareSorted(
            before.stream().sorted(Comparator.comparing(Column::key)).iterator(),
            after.stream().sorted(Comparator.comparing(Column::key)).iterator(),
            emitted::add
        );
        System.out.println("same merge=" + changes.equals(new ChangeSet<>(emitted)));

        // A late ordering error leaves an emitted prefix; the caller handles recovery.
        // Поздняя ошибка порядка оставляет выданный префикс; восстановление выполняет вызывающий код.
        var prefix = new ArrayList<String>();
        try {
            differ.compareSorted(
                List.of(
                    new Column("b", "int", false),
                    new Column("a", "int", false)
                ).iterator(),
                Collections.emptyIterator(),
                change -> prefix.add(change.key())
            );
        } catch (IllegalArgumentException failure) {
            System.out.println(failure.getMessage());
        }
        System.out.println("emitted prefix=" + prefix);

        try {
            differ.compare(
                List.of(new Column("x", "int", false), new Column("x", "int", false)),
                List.of()
            );
        } catch (IllegalArgumentException failure) {
            System.out.println(failure.getMessage());
        }
        try {
            differ.compare(List.of(), List.of(new Column(null, "int", false)));
        } catch (NullPointerException failure) {
            System.out.println(failure.getMessage());
        }
        // A comparator collision is an error, not an implicit identity merge.
        // Коллизия компаратора — ошибка, а не неявное объединение ключей.
        var insensitive = ModelDiffer.<Column, String>of(
            Column::key, String.CASE_INSENSITIVE_ORDER, fields
        );
        try {
            insensitive.compare(
                List.of(new Column("a", "int", false)),
                List.of(new Column("A", "int", false))
            );
        } catch (IllegalArgumentException failure) {
            System.out.println(failure.getMessage());
        }
        try {
            changes.changes().clear();
        } catch (UnsupportedOperationException failure) {
            System.out.println("result is unmodifiable");
        }

        // Projection errors retain the cause and identify the field and model key.
        // Ошибки извлечения сохраняют причину и указывают поле и ключ модели.
        var broken = ModelDiffer.<Column, String>of(
            Column::key, Comparator.naturalOrder(),
            List.of(Field.of("broken", column -> {
                throw new IllegalArgumentException("bad value");
            }))
        );
        try {
            broken.compare(
                List.of(new Column("x", "int", false)),
                List.of(new Column("x", "int", false))
            );
        } catch (IllegalStateException failure) {
            System.out.println(failure.getMessage());
            System.out.println("cause=" + failure.getCause().getMessage());
        }
        try {
            new Change<>("x", ChangeType.MODIFIED,
                new Column("x", "int", false),
                new Column("x", "bigint", false),
                List.of());
        } catch (IllegalArgumentException failure) {
            System.out.println(failure.getMessage());
        }

        System.out.println("All examples completed.");
    }
}


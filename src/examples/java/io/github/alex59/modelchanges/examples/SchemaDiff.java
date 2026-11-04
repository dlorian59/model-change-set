package io.github.alex59.modelchanges.examples;

import io.github.alex59.modelchanges.Field;
import io.github.alex59.modelchanges.ModelDiffer;

import java.util.Comparator;
import java.util.List;

// A schema review compares current and proposed columns before a migration is approved.
// Проверка схемы сравнивает текущие и предлагаемые колонки до согласования миграции.
public final class SchemaDiff {
    private SchemaDiff() { }

    // A qualified name identifies the same column across snapshots.
    // Полное имя определяет одну и ту же колонку в разных снимках.
    public record Column(String qualifiedName, String type, boolean nullable) { }

    // Run with ./gradlew runExample; no database is modified.
    // Запуск: ./gradlew runExample; база данных не изменяется.
    public static void main(String[] args) {
        // Compare only type and nullability; order output by column identity.
        // Сравниваем тип и допустимость null; результат упорядочиваем по ключу колонки.
        var differ = ModelDiffer.<Column, String>of(Column::qualifiedName, Comparator.naturalOrder(), List.of(
                Field.of("type", Column::type),
                Field.of("nullable", Column::nullable)));

        var before = List.of(new Column("orders.id", "bigint", false),
                new Column("orders.note", "text", true),
                new Column("orders.legacy", "text", true));
        var after = List.of(new Column("orders.id", "bigint", false),
                new Column("orders.note", "text", false),
                new Column("orders.created_at", "timestamp", false));

        // Inspect the proposal; applying DDL and checking compatibility are separate decisions.
        // Изучаем предложение; применение DDL и проверка совместимости — отдельные решения.
        differ.compare(before, after).changes().forEach(change -> {
            System.out.println(change.type() + " " + change.key());
            change.fields().forEach(field -> System.out.println("  " + field.name() + ": "
                    + field.before() + " -> " + field.after()));
        });
    }
}

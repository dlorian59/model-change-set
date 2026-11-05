package io.github.alex59.modelchanges;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Reusable field equivalence policies. These policies never normalize the stored
 * values: change reports retain the original objects. They are field policies,
 * not key comparators; key identity rules remain unchanged.
 * <p>Готовые правила равенства полей сохраняют исходные значения в отчёте.
 * Они не заменяют порядок и идентичность ключей.</p>
 */
public final class FieldEquivalence {
    private FieldEquivalence() { }

    /**
     * Compares decimal numeric values, ignoring scale (for example 10.0 and 10.00).
     * No rounding, currency conversion or tolerance is applied. Null equals null
     * and differs from every non-null number.
     * <p>Сравнивает числовое значение без учёта масштаба дроби, округления и конвертации
     * валют. Null равен только null.</p>
     * @return immutable, null-safe decimal equivalence policy
     */
    public static BiPredicate<BigDecimal, BigDecimal> decimalValue() {
        return nullSafe((left, right) -> left.compareTo(right) == 0);
    }

    /**
     * Compares strings using {@link String#equalsIgnoreCase(String)}. No trimming,
     * locale-specific collation or Unicode normalization is performed. Null is
     * equal only to null; an empty string remains distinct from null.
     * <p>Сравнивает строки без учёта регистра через equalsIgnoreCase, без удаления
     * пробелов и нормализации. Null отличается от пустой строки.</p>
     * @return immutable, null-safe text equivalence policy
     */
    public static BiPredicate<String, String> ignoringCase() {
        return nullSafe(String::equalsIgnoreCase);
    }

    /**
     * Adapts a policy for non-null values: two nulls match, one null differs, and
     * the supplied policy is called only for two non-null values. Exceptions from
     * that policy retain the normal field-comparison error behavior.
     * <p>Два null равны, один null означает различие. Пользовательское правило
     * вызывается только для двух ненулевых значений; обработка его ошибок прежняя.</p>
     * @param equivalent comparison of non-null values
     * @param <V> field type
     * @return null-safe wrapper; thread safety depends on the supplied policy
     */
    public static <V> BiPredicate<V, V> nullSafe(BiPredicate<? super V, ? super V> equivalent) {
        Objects.requireNonNull(equivalent, "equivalent");
        return (left, right) -> left == null ? right == null
                : right != null && equivalent.test(left, right);
    }
}

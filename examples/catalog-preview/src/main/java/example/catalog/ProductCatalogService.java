package example.catalog;

import io.github.alex59.modelchanges.ChangeSet;
import io.github.alex59.modelchanges.FieldEquivalence;
import io.github.alex59.modelchanges.ModelDiffer;

// Configure once and reuse; only the business key and relevant fields are selected.
// Настраиваем один раз и используем повторно; выбираем только ключ и значимые поля.
public final class ProductCatalogService {
    private final ModelDiffer<Product, String> differ = ModelDiffer.keyedBy(Product::sku)
            .field("name", Product::name)
            .field("price", Product::price, FieldEquivalence.decimalValue())
            .build();

    // Preview a supplier import before approval; this method does not change either snapshot.
    // Предпросмотр импорта поставщика перед согласованием; оба снимка остаются неизменными.
    public ChangeSet<Product, String> preview(Iterable<Product> current, Iterable<Product> incoming) {
        return differ.compare(current, incoming);
    }
}

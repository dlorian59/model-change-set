package example.catalog;

import java.math.BigDecimal;
import java.time.Instant;

// The SKU identifies a product; importedAt is metadata, not a catalog change.
// Артикул определяет товар; importedAt — метаданные, а не изменение каталога.
public record Product(String sku, String name, BigDecimal price, Instant importedAt) { }

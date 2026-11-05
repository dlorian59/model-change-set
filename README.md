# model-change-set

<details name="readme-language">
<summary><picture><source media="(prefers-color-scheme: dark)" srcset="assets/lang-en-dark.svg"><img src="assets/lang-en-light.svg" width="42" height="26" alt="EN"></picture></summary>

**Version:** [0.3.0 guide](docs/0.3.0/catalog.en.md) · [Versions](docs/index.md) · [API reference](docs/0.3.0/reference.en.md)

Compare two versions of a product catalog, configuration inventory or other Java
model collection. Receive additions, removals and changed fields with their old
and new values, always in a stable key order. Choose the business fields once;
import timestamps and other technical details stay out of the report.

Use it to preview supplier imports, explain configuration updates before approval,
or prepare an audit report. It reads the models you supply and performs no writes.
Java 17+, no runtime dependencies or Spring requirement. [Java support](docs/JAVA-SUPPORT.md).

## Get started in your application

Add the dependency to an existing Java project. These coordinates currently refer
to a locally supplied artifact; a Maven Central release is not available.
[Local artifact preparation and development](CONTRIBUTING.md) are separate from
application integration.

Maven — inside dependencies:

```xml
<dependency>
  <groupId>io.github.59alex</groupId>
  <artifactId>model-change-set</artifactId>
  <version>0.3.0</version>
</dependency>
```

Gradle Kotlin DSL:

```kotlin
repositories { mavenLocal(); mavenCentral() }
dependencies {
    implementation("io.github.59alex:model-change-set:0.3.0")
}
```

In your existing source tree:

```text
src/main/java/example/catalog/
├── Product.java
└── ProductCatalogService.java
```

Product.java — use your existing business model instead if you already have one:

```java
package example.catalog;

import java.math.BigDecimal;
import java.time.Instant;

// The SKU identifies a product; importedAt is metadata, not a catalog change.
public record Product(String sku, String name, BigDecimal price, Instant importedAt) { }
```

ProductCatalogService.java — ready to call from your application service or controller:

```java
package example.catalog;

import io.github.alex59.modelchanges.ChangeSet;
import io.github.alex59.modelchanges.FieldEquivalence;
import io.github.alex59.modelchanges.ModelDiffer;

// Configure once and reuse; only the business key and relevant fields are selected.
public final class ProductCatalogService {
    private final ModelDiffer<Product, String> differ = ModelDiffer.keyedBy(Product::sku)
            .field("name", Product::name)
            .field("price", Product::price, FieldEquivalence.decimalValue())
            .build();

    // Preview a supplier import before approval; this method does not change either snapshot.
    public ChangeSet<Product, String> preview(Iterable<Product> current, Iterable<Product> incoming) {
        return differ.compare(current, incoming);
    }
}
```

Call `new ProductCatalogService().preview(current, incoming)` with two product
collections. The result contains only meaningful differences: a price changing
from 2.00 to 2.50 is reported; 10.0 versus 10.00 and changed import timestamps are
ignored. Inputs may arrive in any order; the report is sorted by SKU.

[Complete runnable catalog example](examples/catalog-preview/src/main/java/example/catalog/CatalogPreview.java) ·
[Step-by-step walkthrough and result](docs/0.3.0/catalog.en.md) ·
[Spring configuration example](docs/0.3.0/spring.en.md)

## What you can do

Each snippet below uses Product and the library imports shown above; `report` is
the result of the service call. Complete imports and assertions are in
[CatalogPreview.java](examples/catalog-preview/src/main/java/example/catalog/CatalogPreview.java).

**Select business fields.** The key defaults to natural order; only selected fields matter.

```java
var differ = ModelDiffer.keyedBy(Product::sku)
    .field("name", Product::name)
    .field("price", Product::price, FieldEquivalence.decimalValue()).build();
```

Result: changes are ordered by SKU, changed fields by declaration order.
[Service source](examples/catalog-preview/src/main/java/example/catalog/ProductCatalogService.java) · [Explanation](docs/0.3.0/catalog.en.md#select-only-the-fields-that-matter).

**Use ready equivalence rules.** Case folding and decimal-scale tolerance are explicit choices.

```java
var names = ModelDiffer.keyedBy(Product::sku)
    .field("name", Product::name, FieldEquivalence.ignoringCase()).build();
```

Result: Notebook and NOTEBOOK match, but null and an empty string differ.
`FieldEquivalence.nullSafe(rule)` also wraps your own non-null comparison.
[Source](examples/catalog-preview/src/main/java/example/catalog/CatalogPreview.java) · [Rules and defaults](docs/0.3.0/catalog.en.md#equality-without-repetitive-null-checks).

**Separate additions, removals and modifications.**

```java
var added = report.added();
var removed = report.removed();
var modified = report.modified();
```

Result: three unmodifiable lists in retained key order; each modification carries
old/new field values. [Source](examples/catalog-preview/src/main/java/example/catalog/CatalogPreview.java) · [Reading the report](docs/0.3.0/catalog.en.md#read-the-report-before-applying-changes).

**Compare membership only.**

```java
var membership = ModelDiffer.keyedBy(Product::sku).build();
```

Result: only new and missing SKUs are reported; price/name differences are ignored.
[Source](examples/catalog-preview/src/main/java/example/catalog/CatalogPreview.java) · [Variants](docs/0.3.0/catalog.en.md#membership-and-custom-order).

**Process already sorted sources one change at a time.**

```java
differ.compareSorted(sortedCurrent, sortedIncoming, change -> audit.accept(change));
```

Here the two inputs are iterators sorted by the same key order; audit is your
Consumer of changes. A complete example uses a result collector and checks it
against snapshot comparison. [Source](examples/catalog-preview/src/main/java/example/catalog/CatalogPreview.java) · [Incremental mode and partial failures](docs/0.3.0/catalog.en.md#larger-sorted-sources).

Duplicate/null keys and inconsistent key ordering are rejected. Comparison is a
preview, not conflict resolution or a database transaction; the application owns
consistent reads, authorization and applying approved changes. An incremental
comparison can deliver a prefix before a later error. [Full contracts](docs/0.3.0/reference.en.md).

[Guide](docs/0.3.0/catalog.en.md) · [MIT](LICENSE)

</details>

<details name="readme-language" open>
<summary><picture><source media="(prefers-color-scheme: dark)" srcset="assets/lang-ru-dark.svg"><img src="assets/lang-ru-light.svg" width="42" height="26" alt="RU"></picture></summary>

**Версия:** [Руководство 0.3.0](docs/0.3.0/catalog.ru.md) · [Все версии](docs/index.md) · [Справочник API](docs/0.3.0/reference.ru.md)

Сравнивайте две версии каталога товаров, набора настроек или других Java-моделей.
Получайте добавления, удаления и изменения полей с прежними и новыми значениями
в стабильном порядке ключей. Один раз укажите значимые бизнес-поля; время импорта
и другие технические сведения не будут засорять отчёт.

Библиотека подходит для предпросмотра импорта поставщика, согласования настроек
перед применением и подготовки отчёта аудита. Она читает переданные модели и
ничего не записывает. Java 17+, без зависимостей времени выполнения и без
обязательного Spring. [Совместимость с Java](docs/JAVA-SUPPORT.md).

## Подключение к существующему приложению

Добавьте зависимость в Java-проект. Сейчас по этим координатам используется
локально предоставленный артефакт; релиза в Maven Central пока нет.
[Подготовка локального артефакта и разработка](CONTRIBUTING.md) вынесены отдельно.

Maven — внутри dependencies:

```xml
<dependency>
  <groupId>io.github.59alex</groupId>
  <artifactId>model-change-set</artifactId>
  <version>0.3.0</version>
</dependency>
```

Gradle Kotlin DSL:

```kotlin
repositories { mavenLocal(); mavenCentral() }
dependencies {
    implementation("io.github.59alex:model-change-set:0.3.0")
}
```

Файлы в существующем приложении:

```text
src/main/java/example/catalog/
├── Product.java
└── ProductCatalogService.java
```

Product.java — если бизнес-модель уже есть, используйте её:

```java
package example.catalog;

import java.math.BigDecimal;
import java.time.Instant;

// Артикул определяет товар; importedAt — метаданные, а не изменение каталога.
public record Product(String sku, String name, BigDecimal price, Instant importedAt) { }
```

ProductCatalogService.java — готовый сервис для вызова из вашего приложения или контроллера:

```java
package example.catalog;

import io.github.alex59.modelchanges.ChangeSet;
import io.github.alex59.modelchanges.FieldEquivalence;
import io.github.alex59.modelchanges.ModelDiffer;

// Настраиваем один раз и используем повторно; выбираем только ключ и значимые поля.
public final class ProductCatalogService {
    private final ModelDiffer<Product, String> differ = ModelDiffer.keyedBy(Product::sku)
            .field("name", Product::name)
            .field("price", Product::price, FieldEquivalence.decimalValue())
            .build();

    // Предпросмотр импорта поставщика перед согласованием; оба снимка остаются неизменными.
    public ChangeSet<Product, String> preview(Iterable<Product> current, Iterable<Product> incoming) {
        return differ.compare(current, incoming);
    }
}
```

Вызовите `new ProductCatalogService().preview(current, incoming)` с двумя коллекциями
товаров. Результат содержит только значимые отличия: изменение цены с 2.00 на 2.50
попадёт в отчёт, а 10.0 вместо 10.00 и новое время импорта — нет. Входные данные
могут идти в любом порядке; результат отсортирован по артикулу.

[Полный запускаемый пример каталога](examples/catalog-preview/src/main/java/example/catalog/CatalogPreview.java) ·
[Пошаговый разбор и результат](docs/0.3.0/catalog.ru.md) ·
[Пример настроек со Spring](docs/0.3.0/spring.ru.md)

## Возможности на примерах

Фрагменты используют Product и импорты библиотеки выше; `report` — результат вызова
сервиса. Полные импорты и проверки находятся в
[CatalogPreview.java](examples/catalog-preview/src/main/java/example/catalog/CatalogPreview.java).

**Выбрать значимые поля.** Ключ по умолчанию имеет естественный порядок; остальные поля не сравниваются.

```java
var differ = ModelDiffer.keyedBy(Product::sku)
    .field("name", Product::name)
    .field("price", Product::price, FieldEquivalence.decimalValue()).build();
```

Результат: изменения идут по артикулу, поля — в порядке объявления.
[Исходник сервиса](examples/catalog-preview/src/main/java/example/catalog/ProductCatalogService.java) · [Разбор](docs/0.3.0/catalog.ru.md#выбрать-только-значимые-поля).

**Использовать готовые правила равенства.** Игнорирование регистра и масштаба десятичной дроби включается явно.

```java
var names = ModelDiffer.keyedBy(Product::sku)
    .field("name", Product::name, FieldEquivalence.ignoringCase()).build();
```

Результат: Notebook и NOTEBOOK равны, но null отличается от пустой строки.
`FieldEquivalence.nullSafe(rule)` также добавляет обработку null к вашему правилу.
[Исходник](examples/catalog-preview/src/main/java/example/catalog/CatalogPreview.java) · [Правила и значения по умолчанию](docs/0.3.0/catalog.ru.md#равенство-без-повторяющихся-проверок-null).

**Разделить добавления, удаления и изменения.**

```java
var added = report.added();
var removed = report.removed();
var modified = report.modified();
```

Результат: три неизменяемых списка с сохранением порядка ключей; у каждого
изменения есть прежние и новые значения полей.
[Исходник](examples/catalog-preview/src/main/java/example/catalog/CatalogPreview.java) · [Чтение отчёта](docs/0.3.0/catalog.ru.md#прочитать-отчёт-перед-применением).

**Сравнить только состав.**

```java
var membership = ModelDiffer.keyedBy(Product::sku).build();
```

Результат: только новые и отсутствующие артикулы; цена и название не учитываются.
[Исходник](examples/catalog-preview/src/main/java/example/catalog/CatalogPreview.java) · [Варианты](docs/0.3.0/catalog.ru.md#состав-и-свой-порядок).

**Читать отсортированные источники по одному изменению.**

```java
differ.compareSorted(sortedCurrent, sortedIncoming, change -> audit.accept(change));
```

Здесь входы — итераторы в одном порядке ключей, audit — ваш Consumer изменений.
Полный пример собирает результат и сверяет его с обычным сравнением снимков.
[Исходник](examples/catalog-preview/src/main/java/example/catalog/CatalogPreview.java) · [Последовательное чтение и частичные ошибки](docs/0.3.0/catalog.ru.md#большие-отсортированные-источники).

Повторные/null-ключи и противоречивый порядок отклоняются. Это предпросмотр, а не
разрешение конфликтов или транзакция БД: согласованные чтения, права доступа и
применение одобренных изменений принадлежат приложению. Последовательное сравнение
может выдать часть результата до поздней ошибки. [Полные контракты](docs/0.3.0/reference.ru.md).

[Руководство](docs/0.3.0/catalog.ru.md) · [MIT](LICENSE)

</details>

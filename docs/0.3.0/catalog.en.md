# Preview a product catalog import

[EN](catalog.en.md) · [RU](catalog.ru.md) · [Guide](en.md) · [API reference](reference.en.md) · [Versions](../index.md) · [README](../../README.md)

A supplier delivers a new catalog. Before applying it, the application needs to
show which products appear, disappear or change. model-change-set compares the
current and proposed collections by SKU and returns that report. It does not
load the catalog, write to a database or decide whether a change is acceptable.

Start with [ProductCatalogService.java](../../examples/catalog-preview/src/main/java/example/catalog/ProductCatalogService.java).
It contains the complete library configuration: one business key and two fields.
The [runnable application](../../examples/catalog-preview/src/main/java/example/catalog/CatalogPreview.java)
provides realistic input, calls that same service and checks the output.


[Connect](#connect-the-service-to-an-existing-project) · [Fields and defaults](#select-only-the-fields-that-matter) ·
[Equality](#equality-without-repetitive-null-checks) · [Report](#read-the-report-before-applying-changes) ·
[Sorted sources](#larger-sorted-sources) · [Errors](#diagnose-input-and-policy-errors)

## Connect the service to an existing project

Use the Maven or Gradle dependency in the [README](../../README.md). The artifact
currently needs to be supplied locally; those coordinates are not a Central
release. The library itself has no runtime dependencies. Java 17 is sufficient;
Spring, a web framework and a database driver are not required.

Copy [Product.java](../../examples/catalog-preview/src/main/java/example/catalog/Product.java)
and [ProductCatalogService.java](../../examples/catalog-preview/src/main/java/example/catalog/ProductCatalogService.java)
into src/main/java/example/catalog, or replace Product with your existing model.
Both files include package declarations and all imports. Construct the service
once, then call preview(current, incoming) from your existing import/approval flow.
No library configuration file, subclass or implementation interface is needed.

The application must read consistent snapshots and enforce access permissions.
For example, read the current product collection at a known revision, show the
report and re-check that revision before an approved write. A report alone does
not protect against another writer changing the catalog in the meantime.

## Select only the fields that matter

```java
private final ModelDiffer<Product, String> differ = ModelDiffer.keyedBy(Product::sku)
    .field("name", Product::name)
    .field("price", Product::price, FieldEquivalence.decimalValue())
    .build();
```

This is the field definition from the complete service. ModelDiffer and
FieldEquivalence come from io.github.alex59.modelchanges. The SKU matches products
between snapshots; selected field names label the report. importedAt is omitted
because an import timestamp changing is not a product change.

Defaults are deliberately small:

| Choice | Default |
| --- | --- |
| Key order | Natural ascending order; the key must implement Comparable consistently with equals |
| Field equality | Objects.deepEquals: nulls and direct array contents supported; other objects use equals |
| Fields | Explicit selection only; none means membership only |
| Duplicate or null model/key | Rejected, never silently replaced |
| Result | Unmodifiable containers sorted by key; fields keep definition order |

The builder is mutable setup state, not safe for concurrent mutation. build()
snapshots the selected fields; adding fields later does not alter previously
built differs. The finished differ is immutable and reusable when model accessors
and comparison policies are safe to share. Neither models nor field values are
deep-copied. Use stable, preferably immutable models during comparison.

## Equality without repetitive null checks

The catalog selects decimalValue() so 10.0 and 10.00 match numerically. It does
not round, convert currencies or introduce a tolerance. To treat display-name
capitalization as irrelevant, explicitly select:

```java
var names = ModelDiffer.keyedBy(Product::sku)
    .field("name", Product::name, FieldEquivalence.ignoringCase()).build();
```

Notebook and NOTEBOOK match; Notebook and " Notebook" do not. This uses
String.equalsIgnoreCase, not locale-specific collation or Unicode normalization.
Both named policies make null equal only to null. A missing value remains different
from zero or an empty string. Reports retain original values without normalization.

For an application-specific rule, delegate the null handling:

```java
var names = ModelDiffer.keyedBy(Product::sku)
    .field("name", Product::name, FieldEquivalence.nullSafe(
        (String left, String right) -> left.strip().equals(right.strip())))
    .build();
```

The inner predicate receives only two non-null values. Unlike these helpers,
raw field(name, getter, predicate) and Field.comparing still pass nulls unchanged
to the custom predicate. This preserves existing domain policies. All three
variants are exercised in [CatalogPreview.java](../../examples/catalog-preview/src/main/java/example/catalog/CatalogPreview.java).

## Read the report before applying changes

The sample current snapshot contains P-1, P-2 and P-3; the proposed snapshot
contains P-1, P-2 and P-4, deliberately in a different order. P-1 has only a new
import time and decimal scale. P-2 changes price from 2.00 to 2.50.

```text
MODIFIED P-2 [price]
REMOVED P-3 []
ADDED P-4 []
added=1; removed=1; modified=1; membership=2; sortedMatches=true
```

With `report` returned by preview:

```java
var additions = report.added();
var removals = report.removed();
var edits = report.modified();
var unchanged = report.isEmpty();
```

A modification contains before/after models and FieldChange entries with name(),
before() and after(). Additions have no before model; removals have no after
model. Those two kinds have no field list because the whole model appeared or
vanished. A changed SKU means removal plus addition, not an inferred rename.

Pass the report to the approval screen or audit writer. The library does not
apply it and does not promise that a deployment/migration represented by it is
safe. Never log sensitive values merely because they occur in a report.

## Membership and custom order

When only product presence matters:

```java
var membership = ModelDiffer.keyedBy(Product::sku).build();
```

The sample then reports P-3 removed and P-4 added, ignoring P-2's price. A custom
key order uses the same builder and engine:

```java
var reverse = ModelDiffer.keyedBy(Product::sku, java.util.Comparator.reverseOrder())
    .field("name", Product::name).build();
```

Keys must retain stable equality, and the comparator must be a total order
consistent with equals. A case-insensitive comparator for case-sensitive keys
can create an ambiguity and is rejected when unequal keys occupy the same position.
Field equivalence helpers must not be substituted for key identity.

## Larger sorted sources

Use compare for ordinary finite collections: it indexes both snapshots before
returning a complete result. The same definition also supports compareSorted for
two iterators already strictly increasing under its key order. This avoids the
two snapshot indexes and delivers changes synchronously one at a time.

```java
var emitted = new java.util.ArrayList<io.github.alex59.modelchanges.Change<Product, String>>();
differ.compareSorted(sortedCurrent, sortedIncoming, emitted::add);
```

The snippet assumes two sorted Iterator<Product> values. The complete
[example](../../examples/catalog-preview/src/main/java/example/catalog/CatalogPreview.java)
sorts its small fixture solely to demonstrate the contract and checks identical
results. For large inputs, obtain sorted cursors from your data source; sorting
entire snapshots in memory removes the memory advantage. The caller closes cursors.

Validation is incremental. A later duplicate, descending key or consumer exception
can occur after earlier changes were delivered; callbacks are not transactional.
Keep side effects behind an application transaction/staging decision if partial
application is unacceptable. No background work, asynchronous cancellation or
automatic resource closing is introduced.

## Diagnose input and policy errors

- Duplicate key: fix the input source; the library does not choose a winner.
- Null model/key: provide an actual model with a stable identity; nullable fields are allowed.
- Duplicate field name: remove the repeated declaration before build().
- Cannot compare field: inspect the original cause; a getter or custom comparison failed. The error names the key and field.
- Unexpected changes: check the selected fields and policy. Default equality is exact, including BigDecimal scale; decimalValue() is opt-in.
- Old report became stale: re-read/revalidate the source revision before applying changes.

## Continue from here

[Local run and copied-consumer verification](../../examples/catalog-preview/README.md) ·
[Detailed comparison modes](en.md) · [Complete API reference](reference.en.md) ·
[Spring configuration example](spring.en.md) · [Java support](java.en.md).

The important contract is a stable key, explicit business equality and a separate
application decision about writes. Snapshot mode returns the whole report;
sorted mode trades that boundary for incremental delivery and possible partial output.

[← Versions](../index.md) · [README](../../README.md)

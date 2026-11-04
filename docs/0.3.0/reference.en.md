# model-change-set 0.3.0 — Complete API and behavior reference

[← Usage guide](en.md) · [Versions](../index.md) · [README](../../README.md) · [Русский](reference.ru.md)

This reference covers every public library type and operation. Start with the [runnable full example](examples/Example.java) or the [Spring configuration scenario](spring.en.md); the tables distinguish computed results from manually constructed data containers.

## Public API

| API and source | Usage in a model-comparison flow | Contract and edge cases |
| --- | --- | --- |
| [ModelDiffer.of(key, order, fields)](../../src/main/java/io/github/alex59/modelchanges/ModelDiffer.java) | `ModelDiffer.<Column,String>of(Column::key, Comparator.naturalOrder(), fields)` | Stable non-null keys; total comparator consistent with equals; unique nonblank field names. Configuration is copied. No field discovery. |
| [ModelDiffer.compare(before, after)](../../src/main/java/io/github/alex59/modelchanges/ModelDiffer.java) | `var changes = differ.compare(before, after)` | Finite Iterables in any order; indexes both once before comparing fields. Empty snapshots are valid; an empty field list compares membership only. |
| [ModelDiffer.compareSorted(before, after, consumer)](../../src/main/java/io/github/alex59/modelchanges/ModelDiffer.java) | `differ.compareSorted(sortedBefore, sortedAfter, emitted::add)` | Strictly increasing under the configured comparator. Synchronous callbacks, one current entry per input; no background work or automatic closing. Late errors can leave a prefix. |
| [Field.of(name, extractor)](../../src/main/java/io/github/alex59/modelchanges/Field.java) | `Field.of("type", Column::type)` | Default Objects.deepEquals: direct null and array values supported, other objects use equals. No string trimming, case folding or domain normalization. |
| [Field.comparing(name, extractor, equivalent)](../../src/main/java/io/github/alex59/modelchanges/Field.java) | `Field.comparing("amount", Price::amount, nullAwareNumericEquality)` | The predicate receives null unchanged; true suppresses the change. The report retains original values rather than normalized ones. |
| [Field.name()](../../src/main/java/io/github/alex59/modelchanges/Field.java) | `amount.name()` | Exactly the supplied nonblank name; names are case-sensitive and not trimmed. Field comparison itself is internal, not public API. |
| [Change<T,K> constructor and accessors](../../src/main/java/io/github/alex59/modelchanges/Change.java) | `change.key()`, `type()`, `before()`, `after()`, `fields()` | Constructor validates structural shape shown below and copies fields. It does not re-run the differ or verify model identity, field uniqueness, or semantic inequality. |
| [ChangeType](../../src/main/java/io/github/alex59/modelchanges/ChangeType.java) | `ChangeType.ADDED`, `REMOVED`, `MODIFIED` | A key change is a removal plus addition. There is no unchanged, renamed or migration-safe classification. Enum valueOf/values follow standard Java semantics. |
| [FieldChange constructor and accessors](../../src/main/java/io/github/alex59/modelchanges/FieldChange.java) | `new FieldChange("type", "int", "bigint")`; `name()`, `before()`, `after()` | Name must be non-null and nonblank. Before/after are nullable Objects retained by reference; construction does not require them to be unequal. |
| [ChangeSet<T,K> constructor and changes()](../../src/main/java/io/github/alex59/modelchanges/ChangeSet.java) | `new ChangeSet<>(emitted)`; `changes.changes()` | Copies the supplied list in its existing order. Manual construction does not sort, merge duplicate keys, or validate a relationship between separate changes. |
| [ChangeSet.isEmpty/added/removed/modified](../../src/main/java/io/github/alex59/modelchanges/ChangeSet.java) | `changes.isEmpty()`; `changes.modified()` | Selectors return unmodifiable lists preserving the stored order. Key ordering is guaranteed for ModelDiffer output, not arbitrary manually assembled reports. |

`Column`, `Price`, `fields`, `differ`, `changes` and `emitted` are defined in [Example.java](examples/Example.java). `sortedBefore/sortedAfter` mean iterators sorted with the same comparator, and `nullAwareNumericEquality` is the predicate shown in that example. Records also provide standard Java `equals`, `hashCode` and `toString`; these do not override equality rules of their contained values.

## Change shape

Key/type/field-list must be non-null; field-list entries must be non-null.

| Type | before | after | fields |
| --- | --- | --- |
| ADDED | null | model | empty |
| REMOVED | model | null | empty |
| MODIFIED | model | model | nonempty |

The constructor protects the shape, not the truth of a manually written report. For example, input order is preserved here:

```java
// Containers preserve caller order; ModelDiffer is responsible for computing and sorting changes.
// Контейнеры сохраняют порядок вызывающего кода; ModelDiffer вычисляет и сортирует изменения.
var manual = new ChangeSet<String, String>(List.of(
    new Change<>("z", ChangeType.ADDED, null, "last", List.of()),
    new Change<>("a", ChangeType.ADDED, null, "first", List.of())));
System.out.println(manual.added().stream().map(Change::key).toList()); // [z, a]
```

## Configuration, ownership and lifecycle

| Concern | Behavior |
| --- | --- |
| Defaults | Only field equivalence defaults to `Objects.deepEquals`; key extraction, key ordering and field list are always explicit. There are no environment/property settings, scheduler, cache, persistence or background threads. |
| Models and values | Caller-owned; no deep copy. Do not mutate keys, model fields or array values during comparison or while relying on a retained report. |
| Collections | Field definitions and result lists are copied into immutable containers. Input collections are not modified. Iterators are consumed but never closed by the library. |
| Concurrency | A differ has no per-call shared mutable state. The caller must supply thread-safe/pure functions and stable data before sharing it. |
| Failure and retry | Batch compare returns a whole result or throws, but callback side effects are not rolled back. Streaming can emit before failing; retries may repeat external writes. |
| Memory/work | Batch: `O(n log n + m log m + min(n,m) × f)` plus result construction; memory `O(n + m + result)`. Sorted merge: `O(n + m + min(n,m) × f)`, working memory `O(f)`. Assumes constant-cost comparisons/projections. |
| Exclusions | No rename inference, graph traversal, business compatibility classification, automatic migrations, transactional apply, optimistic locking or audit persistence. |

## Errors and recovery

| Error/symptom | Cause | Action |
| --- | --- | --- |
| `NullPointerException` | Null input, model, key, callback, field definition, name, or list element. Nullable projected values are allowed. | Remove invalid structure; do not replace a missing model with a model containing null fields. |
| `Duplicate key in before/after` | Repeated identity in one snapshot, even for identical models. | Choose a real stable/composite identity or correct the source; do not silently deduplicate. |
| `Comparator treats unequal keys as identical` | Comparator returns zero for unequal keys, within or across snapshots. | Make ordering consistent with equals; case-insensitive ordering alone does not change identity. |
| `Keys in before/after are not strictly increasing` | The sorted API encountered a descending key. | Sort using exactly the differ comparator; compensate or discard an already emitted report prefix. |
| `Cannot compare field … at key …` / `IllegalStateException` | A field extractor or equality predicate threw RuntimeException. | Inspect getCause(); fix that projection or policy. Key/comparator/iterator/consumer failures propagate without this wrapper. |
| `Field name must not be blank`, `Duplicate field name` | Blank names or repeated configured names. | Use explicit unique labels; names are case-sensitive and preserved verbatim. |
| `Invalid change shape for …` | A manually constructed Change violates the null/list shape. | Use the shape table; prefer the differ when deriving a change from snapshots. |
| `UnsupportedOperationException` | Attempt to edit a result list or its iterator. | Create an application-owned copy if editing a presentation; copy mutable values separately when needed. |

## Spring and AOP scope

Use the [typed bean example](spring.en.md). There is no aspect, annotation registry, proxy requirement or automatic retry. `ModelDiffer` is final: keep a timing/audit/transaction policy on the application service that owns snapshots or writes, rather than trying to subclass-proxy the core. If that service uses Spring AOP, calls must cross its proxy; self-invocation bypasses advice. These are [Spring proxy constraints](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html), not extra guarantees from this library.

## Build and verification

From a checkout on a full JDK 17+ (or select an installed JDK with `-PtestJavaVersion=17`):

```sh
./gradlew test runExample runSpringExample
```

The [existing contract tests](../../src/test/java/io/github/alex59/modelchanges/ModelDifferTest.java) cover duplicates, collisions, ordering, nulls, immutable containers, shape validation and incremental failures. Run the complete plain-Java guide with the [documented commands](en.md#7-run-the-complete-example-and-diagnose-failures). For dependency resolution, JDK setup and earlier incompatible releases, see [troubleshooting](en.md#7-run-the-complete-example-and-diagnose-failures) and [Java/version matrix](java.en.md).

[← Usage guide](en.md)

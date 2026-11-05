# 0.3.0 — review configuration changes with Spring

[← Usage guide](en.md) · [Versions](../index.md) · [README](../../README.md) · [Русский](spring.ru.md)

[Complete API/configuration/error reference](reference.en.md)

## Situation

An operations screen compares deployed worker policies with a proposed configuration.
It should show removals, additions and changed execution limits before anyone approves a
rollout. An observation timestamp must not restart a worker whose business settings are unchanged.
This is a reusable integration example; it previews changes and does not write configuration.

## Setup

In an existing Spring application, use the ordinary `model-change-set:0.3.0` dependency
from the [installation guide](en.md#1-install-and-prepare-the-example). No separate starter
is needed: the application already knows the model, its stable key and relevant fields.
The runnable example uses Spring Context with the Boot 4.1.2 dependency BOM; Spring stays
on the example classpath and is absent from the published core's runtime dependencies.

From this repository on a full JDK 17 or newer:

```sh
./gradlew runSpringExample
```

The same example is part of `./gradlew check`. Use `-PtestJavaVersion=17` to choose an
installed JDK explicitly.

## Typed configuration

[WorkerPolicy](../../src/springExamples/java/io/github/alex59/modelchanges/examples/spring/WorkerPolicy.java) is an immutable record containing a key,
parallelism, timeout and observation time. The complete
[PolicyConfiguration](../../src/springExamples/java/io/github/alex59/modelchanges/examples/spring/PolicyConfiguration.java) needs only two bean methods:

```java
package io.github.alex59.modelchanges.examples.spring;

import io.github.alex59.modelchanges.ModelDiffer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class PolicyConfiguration {
    @Bean
    public ModelDiffer<WorkerPolicy, String> workerPolicyDiffer() {
        // Declare exactly what matters for a rollout; observedAt is deliberately omitted.
        // Явно задаём поля, влияющие на применение настроек; observedAt намеренно исключён.
        return ModelDiffer.keyedBy(WorkerPolicy::key)
            .field("parallelism", WorkerPolicy::parallelism)
            .field("timeout", WorkerPolicy::timeout)
            .build();
    }

    @Bean
    public ConfigurationSync configurationSync(ModelDiffer<WorkerPolicy, String> workerPolicyDiffer) {
        // Generic types keep this injection specific to worker policies without a registry or casts.
        // Типы ограничивают внедрение настройками обработчиков; реестр и приведения типов не нужны.
        return new ConfigurationSync(workerPolicyDiffer);
    }
}
```

[ConfigurationSync](../../src/springExamples/java/io/github/alex59/modelchanges/examples/spring/ConfigurationSync.java) receives
`ModelDiffer<WorkerPolicy, String>` by constructor and delegates its preview to
`compare(current, desired)`. Other model types can use their own typed beans; if several
beans share exactly the same generic type, use ordinary Spring `@Qualifier` or `@Primary`.
There is no reflective field discovery, raw-type registry or hidden model selection.

## From snapshots to a reviewable result

[ConfigurationSyncApplication](../../src/springExamples/java/io/github/alex59/modelchanges/examples/spring/ConfigurationSyncApplication.java) creates a real
Spring context and supplies two immutable snapshots. `legacy` is removed, `push` is added,
and `mail` changes parallelism and timeout. `audit` differs only in `observedAt` and is excluded.

```text
REMOVED legacy
MODIFIED mail
  parallelism: 4 -> 8
  timeout: PT30S -> PT45S
ADDED push
Converged: no changes
```

The executable checks candidate keys, change types, changed-field names, unchanged inputs
and the empty result when current values already match desired values. The final line
simulates a fresh matching snapshot; the demonstration itself performs no rollout.

## Extend the scenario deliberately

| Requirement | Configuration and runnable source |
| --- | --- |
| Ignore audit metadata | Omit `observedAt` in [PolicyConfiguration](../../src/springExamples/java/io/github/alex59/modelchanges/examples/spring/PolicyConfiguration.java) |
| Choose only additions/removals/updates for a review screen | `added()`, `removed()`, `modified()` in the [full core example](examples/Example.java) |
| Ignore decimal scale or define null semantics | `Field.comparing` in the [full core example](examples/Example.java) |
| Review database schema snapshots | [SchemaDiff](../../src/examples/java/io/github/alex59/modelchanges/examples/SchemaDiff.java), run `./gradlew runExample` |
| Process a large, already sorted inventory | `compareSorted` in the [full core example](examples/Example.java); [streaming contract](en.md#5-merge-sorted-sources-incrementally) |

## Boundaries

Obtain consistent snapshots in the host application. The preview is not a transaction,
optimistic lock, migration validator or a guarantee that applying the proposal is safe.
Check versions again before writing; decide how removals are approved, how changes are
applied atomically, and how failed application is recovered. Result collections are
immutable, but referenced model/field values are not deep-copied. A streaming consumer
can receive a prefix before a later error; batch `compare` returns a complete result or throws.

[← Usage guide](en.md)

# 0.3.0 — проверка изменений конфигурации в Spring

[← Руководство](ru.md) · [Версии](../index.md) · [README](../../README.md) · [English](spring.en.md)

[Полный справочник API/настроек/ошибок](reference.ru.md)

## Ситуация

Административный экран сравнивает действующие настройки обработчиков с предложенной
конфигурацией. До согласования применения нужно показать удаления, добавления и новые
лимиты выполнения. Изменение времени наблюдения не должно перезапускать обработчик,
если его бизнес-настройки остались прежними. Это переиспользуемый пример интеграции:
он показывает изменения и не записывает конфигурацию.

## Подключение

В существующем Spring-приложении используйте обычную зависимость `model-change-set:0.3.0`
из [руководства по установке](ru.md#1-установка-и-подготовка-примера). Отдельный стартер
не нужен: приложение уже знает модель, стабильный ключ и значимые поля. Запускаемый
пример подключает Spring Context через BOM Boot 4.1.2; Spring остаётся зависимостью
примера и не попадает в runtime-зависимости основной библиотеки.

Из корня репозитория на полном JDK 17 или новее:

```sh
./gradlew runSpringExample
```

Этот же пример входит в `./gradlew check`. Параметр `-PtestJavaVersion=17` явно выбирает
установленный JDK.

## Типизированная конфигурация

[WorkerPolicy](../../src/springExamples/java/io/github/alex59/modelchanges/examples/spring/WorkerPolicy.java) — неизменяемая запись с ключом, параллелизмом,
таймаутом и временем наблюдения. Полной
[PolicyConfiguration](../../src/springExamples/java/io/github/alex59/modelchanges/examples/spring/PolicyConfiguration.java) достаточно двух методов beans:

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

[ConfigurationSync](../../src/springExamples/java/io/github/alex59/modelchanges/examples/spring/ConfigurationSync.java) принимает
`ModelDiffer<WorkerPolicy, String>` через конструктор и передаёт снимки в
`compare(current, desired)`. Для других моделей определите свои типизированные beans.
Если несколько beans имеют одинаковые generic-типы, используйте обычные Spring
`@Qualifier` или `@Primary`. Реестра с raw types, рефлексивного поиска полей и скрытого
выбора модели нет.

## От снимков к результату для согласования

[ConfigurationSyncApplication](../../src/springExamples/java/io/github/alex59/modelchanges/examples/spring/ConfigurationSyncApplication.java) создаёт
настоящий контекст Spring и передаёт два неизменяемых снимка. `legacy` удаляется,
`push` добавляется, у `mail` меняются параллелизм и таймаут. У `audit` изменён только
`observedAt`, поэтому он не попадает в отчёт.

```text
REMOVED legacy
MODIFIED mail
  parallelism: 4 -> 8
  timeout: PT30S -> PT45S
ADDED push
Converged: no changes
```

Пример проверяет ключи кандидатов, виды изменений, названия изменённых полей,
неизменность входа и пустой результат при совпадении текущих и желаемых значений.
Последняя строка моделирует новый совпадающий снимок; само применение настроек
демонстрация не выполняет.

## Адаптация к своей задаче

| Требование | Настройка и запускаемый исходник |
| --- | --- |
| Игнорировать метаданные аудита | Исключить `observedAt` в [PolicyConfiguration](../../src/springExamples/java/io/github/alex59/modelchanges/examples/spring/PolicyConfiguration.java) |
| Показывать на экране только добавления, удаления или изменения | `added()`, `removed()`, `modified()` в [полном примере](examples/Example.java) |
| Игнорировать масштаб цены или задать семантику null | `Field.comparing` в [полном примере](examples/Example.java) |
| Проверять снимки схемы базы | [SchemaDiff](../../src/examples/java/io/github/alex59/modelchanges/examples/SchemaDiff.java), запуск `./gradlew runExample` |
| Обрабатывать большой уже отсортированный каталог | `compareSorted` в [полном примере](examples/Example.java); [контракт последовательной обработки](ru.md#5-последовательное-сравнение-отсортированных-источников) |

## Границы применения

Приложение получает согласованные снимки самостоятельно. Предварительный отчёт не
заменяет транзакцию, optimistic lock, проверку миграции и решение о безопасности
применения. Перед записью повторно проверьте версии; определите согласование удалений,
атомарное применение и восстановление после ошибки. Коллекции результата неизменяемы,
но модели и значения полей не копируются глубоко. Последовательный обработчик может
получить часть изменений до поздней ошибки; пакетный `compare` возвращает полный
результат либо выбрасывает исключение.

[← Руководство](ru.md)

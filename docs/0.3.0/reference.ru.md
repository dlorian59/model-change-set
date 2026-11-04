# model-change-set 0.3.0 — Полный справочник API и поведения

[← Руководство](ru.md) · [Версии](../index.md) · [README](../../README.md) · [English](reference.en.md)

Справочник охватывает все публичные типы и операции библиотеки. Начните с [полного запускаемого примера](examples/Example.java) или [сценария Spring](spring.ru.md); таблицы различают вычисленный результат и вручную созданные контейнеры данных.

## Публичный API

| API и исходник | Использование при сравнении моделей | Контракт и границы |
| --- | --- | --- |
| [ModelDiffer.of(key, order, fields)](../../src/main/java/io/github/alex59/modelchanges/ModelDiffer.java) | `ModelDiffer.<Column,String>of(Column::key, Comparator.naturalOrder(), fields)` | Стабильные ключи без null; полный порядок согласован с equals; уникальные непустые имена полей. Настройки копируются. Автопоиска полей нет. |
| [ModelDiffer.compare(before, after)](../../src/main/java/io/github/alex59/modelchanges/ModelDiffer.java) | `var changes = differ.compare(before, after)` | Конечные Iterable в любом порядке; оба индексируются по одному разу до сравнения полей. Пустые снимки допустимы; пустой набор полей сравнивает только состав. |
| [ModelDiffer.compareSorted(before, after, consumer)](../../src/main/java/io/github/alex59/modelchanges/ModelDiffer.java) | `differ.compareSorted(sortedBefore, sortedAfter, emitted::add)` | Строго возрастающий порядок по заданному компаратору. Синхронные callbacks, один текущий элемент каждого входа; без фоновой работы и автоматического закрытия. Поздняя ошибка может оставить префикс. |
| [Field.of(name, extractor)](../../src/main/java/io/github/alex59/modelchanges/Field.java) | `Field.of("type", Column::type)` | По умолчанию Objects.deepEquals: поддержаны null и значения-массивы; остальные объекты используют equals. Нет trim, смены регистра или предметной нормализации. |
| [Field.comparing(name, extractor, equivalent)](../../src/main/java/io/github/alex59/modelchanges/Field.java) | `Field.comparing("amount", Price::amount, nullAwareNumericEquality)` | Предикат получает null без изменений; true исключает изменение из отчёта. В отчёте сохраняются исходные, а не нормализованные значения. |
| [Field.name()](../../src/main/java/io/github/alex59/modelchanges/Field.java) | `amount.name()` | Именно переданное непустое имя; регистр значим, пробелы не обрезаются. Сам метод сравнения поля внутренний, не публичный API. |
| [Change<T,K> constructor and accessors](../../src/main/java/io/github/alex59/modelchanges/Change.java) | `change.key()`, `type()`, `before()`, `after()`, `fields()` | Конструктор проверяет структуру из таблицы ниже и копирует fields. Он не запускает differ и не проверяет ключ модели, уникальность полей или смысловое неравенство. |
| [ChangeType](../../src/main/java/io/github/alex59/modelchanges/ChangeType.java) | `ChangeType.ADDED`, `REMOVED`, `MODIFIED` | Смена ключа — удаление и добавление. Нет категорий unchanged, renamed или безопасной миграции. valueOf/values имеют обычную семантику Java enum. |
| [FieldChange constructor and accessors](../../src/main/java/io/github/alex59/modelchanges/FieldChange.java) | `new FieldChange("type", "int", "bigint")`; `name()`, `before()`, `after()` | Имя обязано быть non-null и непустым. Before/after — допускающие null Objects по ссылке; конструктор не требует их неравенства. |
| [ChangeSet<T,K> constructor and changes()](../../src/main/java/io/github/alex59/modelchanges/ChangeSet.java) | `new ChangeSet<>(emitted)`; `changes.changes()` | Копирует переданный список в исходном порядке. Ручное создание не сортирует, не объединяет одинаковые ключи и не проверяет связь отдельных изменений. |
| [ChangeSet.isEmpty/added/removed/modified](../../src/main/java/io/github/alex59/modelchanges/ChangeSet.java) | `changes.isEmpty()`; `changes.modified()` | Выборки возвращают неизменяемые списки в сохранённом порядке. Порядок ключей гарантирован для результата ModelDiffer, а не произвольно составленных вручную отчётов. |

`Column`, `Price`, `fields`, `differ`, `changes` и `emitted` определены в [Example.java](examples/Example.java). `sortedBefore/sortedAfter` обозначают итераторы с тем же порядком, а `nullAwareNumericEquality` — показанный там предикат. Records также имеют стандартные `equals`, `hashCode` и `toString` Java; они не меняют правила равенства содержащихся значений.

## Структура изменения

Ключ, тип и список полей не могут быть null; элементы списка также non-null.

| Тип | before | after | fields |
| --- | --- | --- |
| ADDED | null | модель | пустой |
| REMOVED | модель | null | пустой |
| MODIFIED | модель | модель | непустой |

Конструктор защищает структуру, но не достоверность написанного вручную отчёта. Например, здесь сохраняется порядок входа:

```java
// Containers preserve caller order; ModelDiffer is responsible for computing and sorting changes.
// Контейнеры сохраняют порядок вызывающего кода; ModelDiffer вычисляет и сортирует изменения.
var manual = new ChangeSet<String, String>(List.of(
    new Change<>("z", ChangeType.ADDED, null, "last", List.of()),
    new Change<>("a", ChangeType.ADDED, null, "first", List.of())));
System.out.println(manual.added().stream().map(Change::key).toList()); // [z, a]
```

## Настройка, владение и жизненный цикл

| Аспект | Поведение |
| --- | --- |
| Значения по умолчанию | Только равенство поля имеет default `Objects.deepEquals`; извлечение ключа, порядок и список полей всегда задаются явно. Нет environment/properties, планировщика, кэша, хранения или фоновых потоков. |
| Модели и значения | Принадлежат вызывающему коду; глубокого копирования нет. Не меняйте ключи, поля и массивы во время сравнения и пока полагаетесь на сохранённый отчёт. |
| Коллекции | Определения полей и результирующие списки копируются в неизменяемые контейнеры. Входные коллекции не изменяются. Итераторы потребляются, но библиотека их не закрывает. |
| Конкурентность | У differ нет общего изменяемого состояния вызова. Для совместного использования нужны чистые/потокобезопасные функции и стабильные данные. |
| Ошибка и повтор | Пакетное сравнение возвращает целый результат или ошибку, но побочные эффекты callbacks не откатываются. Потоковый вариант может выдать префикс; повтор способен повторить внешние записи. |
| Работа и память | Пакетный режим: `O(n log n + m log m + min(n,m) × f)` плюс построение результата; память `O(n + m + result)`. Sorted merge: `O(n + m + min(n,m) × f)`, рабочая память `O(f)`. Предполагается постоянная стоимость сравнения/извлечения. |
| За границами | Нет определения переименований, обхода графа, оценки бизнес-совместимости, миграций, транзакционного применения, optimistic lock и хранения аудита. |

## Ошибки и восстановление

| Ошибка/симптом | Причина | Действие |
| --- | --- | --- |
| `NullPointerException` | Null во входе, модели, ключе, callback, определении поля, имени или элементе списка. Значение извлечённого поля может быть null. | Исправьте структуру; отсутствующая модель не равна модели с null в поле. |
| `Duplicate key in before/after` | Одинаковый ключ в одном снимке, даже для одинаковых моделей. | Используйте настоящий стабильный/составной ключ либо исправьте источник; не удаляйте дубликат молча. |
| `Comparator treats unequal keys as identical` | Компаратор возвращает ноль для неравных ключей внутри снимка или между снимками. | Согласуйте порядок с equals; регистронезависимый компаратор сам по себе не меняет идентичность. |
| `Keys in before/after are not strictly increasing` | Последовательный API встретил убывающий ключ. | Сортируйте тем же компаратором; компенсируйте или отбросьте уже выданную часть отчёта. |
| `Cannot compare field … at key …` / `IllegalStateException` | Функция поля или предикат равенства выбросили RuntimeException. | Изучите getCause() и исправьте функцию/правило. Ошибки ключа, компаратора, итератора и consumer передаются без этой обёртки. |
| `Field name must not be blank`, `Duplicate field name` | Пустые имена или повторяющиеся имена настроенных полей. | Используйте явные уникальные имена; регистр значим, строка сохраняется как передана. |
| `Invalid change shape for …` | Ручной Change нарушает структуру null и списка полей. | Сверьтесь с таблицей структуры; для вычисления изменений из снимков используйте differ. |
| `UnsupportedOperationException` | Попытка изменить результирующий список или его итератор. | Сделайте собственную копию для редактирования представления; изменяемые значения при необходимости копируйте отдельно. |

## Spring и границы AOP

Используйте [типизированный bean](spring.ru.md). Аспекта, реестра аннотаций, обязательного прокси и автоматического повтора нет. `ModelDiffer` — final: политику времени, аудита или транзакции размещайте на сервисе приложения, который получает снимки или пишет данные, а не на subclass-прокси ядра. При Spring AOP вызов должен проходить через прокси сервиса; self-invocation обходит advice. Это [ограничения Spring-прокси](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html), а не дополнительные гарантии библиотеки.

## Сборка и проверка

Из репозитория на полном JDK 17+ (установленный JDK можно выбрать через `-PtestJavaVersion=17`):

```sh
./gradlew test runExample runSpringExample
```

[Существующие контрактные тесты](../../src/test/java/io/github/alex59/modelchanges/ModelDifferTest.java) проверяют дубликаты, коллизии, порядок, null, неизменяемые контейнеры, структуру и поздние ошибки. Полный обычный Java-пример запускается [командами руководства](ru.md#7-запуск-полного-примера-и-диагностика). Решение ошибок зависимостей, выбора JDK и несовместимых прежних версий: [диагностика](ru.md#7-запуск-полного-примера-и-диагностика), [матрица Java/версий](java.ru.md).

[← Руководство](ru.md)

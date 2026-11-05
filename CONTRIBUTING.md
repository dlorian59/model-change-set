# Development and local artifacts / Разработка и локальные артефакты

[README](README.md) · [Catalog guide / Каталог](docs/0.3.0/catalog.en.md) · [Русский](docs/0.3.0/catalog.ru.md) · [Versions / Версии](docs/index.md)

## English

The library is not published to Maven Central. To prepare the artifact locally,
use an existing source checkout and an installed JDK 17+:

```sh
./gradlew build publishToMavenLocal --no-daemon --max-workers=2
./gradlew -p examples/catalog-preview run
```

The first command tests the library, runs the existing Spring configuration
example and places the JAR, POM and Gradle metadata in Maven Local. The second
command resolves that artifact from a separate application build. It does not
use includeBuild, project dependencies or compile the library source directly.
The published library has no Spring dependency.

For a real copy/paste check, copy examples/catalog-preview to another directory
without its build, target or .gradle directories. Run Gradle 9.8.2 there, or Maven
with the supplied pom.xml. Java 17 APIs are used even on newer JDKs.

```sh
./gradlew -p examples/catalog-preview run -PexampleJava=17
mvn -f examples/catalog-preview/pom.xml compile
```

The full source and expected output are in the [example guide](examples/catalog-preview/README.md).
For library validation on another installed runtime, use -PtestJavaVersion=21,
25 or 27; this does not change --release 17. Do not claim a runtime was tested
merely because its selector exists.

Current change verification (2026-11-05): library build/publication on Java 17,
three focused builder/equivalence regression tests plus the existing suite,
existing Spring example, copied independent Gradle consumer on Java 17, and
Maven compilation of the same application succeeded. The consumer checks real
catalog additions/removals/price changes, equivalence rules, membership and sorted
mode. No remote registry publication or new multi-JDK test matrix was performed.

## Русский

Библиотека пока не опубликована в Maven Central. Для подготовки локального
артефакта нужны исходники и установленный JDK 17+:

```sh
./gradlew build publishToMavenLocal --no-daemon --max-workers=2
./gradlew -p examples/catalog-preview run
```

Первая команда проверяет библиотеку, запускает прежний пример настроек со Spring
и помещает JAR, POM и метаданные Gradle в Maven Local. Вторая получает артефакт
через отдельную сборку приложения. Подстановки соседнего проекта и прямой
компиляции исходников библиотеки в приложении нет. Публикуемая библиотека не
зависит от Spring.

Чтобы проверить перенос примера, скопируйте examples/catalog-preview в другой
каталог без build, target и .gradle. Используйте там Gradle 9.8.2 либо Maven с
готовым pom.xml. Даже на новой JVM компиляция ограничена API Java 17.

```sh
./gradlew -p examples/catalog-preview run -PexampleJava=17
mvn -f examples/catalog-preview/pom.xml compile
```

[Разбор примера](examples/catalog-preview/README.md) содержит исходники и результат.
Для другой установленной JVM у библиотеки есть -PtestJavaVersion=21, 25 или 27;
цель --release 17 сохраняется. Наличие параметра само по себе не доказывает
выполненную проверку конкретной JVM.

Проверка изменения от 2026-11-05: успешны сборка/локальная публикация на Java 17,
три проверки построителя и правил равенства вместе с прежними тестами, прежний
Spring-пример, скопированное отдельное Gradle-приложение на Java 17 и компиляция
того же приложения Maven. Пример проверяет добавления, удаления, цену, правила
равенства, состав и последовательный режим. Удалённой публикации и нового прогона
всей матрицы JDK не было.

[← README](README.md)

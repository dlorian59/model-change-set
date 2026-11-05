# Catalog preview / Предпросмотр каталога

[English walkthrough](../../docs/0.3.0/catalog.en.md) · [Разбор на русском](../../docs/0.3.0/catalog.ru.md) · [Versions / Версии](../../docs/index.md) · [README](../../README.md)

## English

This is an independent Java application. After preparing the library artifact as
shown in [CONTRIBUTING](../../CONTRIBUTING.md), run from the library checkout:

```sh
./gradlew -p examples/catalog-preview run -PexampleJava=17
```

Read [Product.java](src/main/java/example/catalog/Product.java), then
[ProductCatalogService.java](src/main/java/example/catalog/ProductCatalogService.java).
These two files are all the application needs for the comparison itself.
[CatalogPreview.java](src/main/java/example/catalog/CatalogPreview.java) supplies
sample snapshots and asserts ordering, price equality, selected fields, report
filters, custom/null-safe text rules, membership and sorted comparison.

```text
MODIFIED P-2 [price]
REMOVED P-3 []
ADDED P-4 []
added=1; removed=1; modified=1; membership=2; sortedMatches=true
```

Copy the directory without generated build/target/.gradle directories to verify
integration separately. [build.gradle.kts](build.gradle.kts) resolves the Maven
Local artifact; [pom.xml](pom.xml) provides an equivalent Maven dependency and
Java 17 compilation. Neither build substitutes a sibling library project.
Maven compilation uses the [compiler plugin's release setting](https://maven.apache.org/plugins-archives/maven-compiler-plugin-3.14.1/).

```sh
mvn compile
java -cp target/classes:$HOME/.m2/repository/io/github/59alex/model-change-set/0.3.0/model-change-set-0.3.0.jar example.catalog.CatalogPreview
```

The classpath command assumes Maven's default local repository and a POSIX shell.
The example performs no network requests or writes to a catalog. An assertion
failure exits unsuccessfully; printed success is backed by result checks.

## Русский

Это отдельное Java-приложение. Подготовьте артефакт по
[CONTRIBUTING](../../CONTRIBUTING.md), затем из каталога библиотеки выполните:

```sh
./gradlew -p examples/catalog-preview run -PexampleJava=17
```

Читайте [Product.java](src/main/java/example/catalog/Product.java), затем
[ProductCatalogService.java](src/main/java/example/catalog/ProductCatalogService.java).
Для самого сравнения приложению нужны только эти два файла.
[CatalogPreview.java](src/main/java/example/catalog/CatalogPreview.java) задаёт
снимки и проверяет порядок, цену, выбор полей, фильтры отчёта, собственные правила
равенства и null, состав и последовательное сравнение.

```text
MODIFIED P-2 [price]
REMOVED P-3 []
ADDED P-4 []
added=1; removed=1; modified=1; membership=2; sortedMatches=true
```

Для отдельной проверки перенесите каталог без созданных build/target/.gradle.
[build.gradle.kts](build.gradle.kts) получает JAR из Maven Local;
[pom.xml](pom.xml) задаёт ту же зависимость и компиляцию Java 17 через Maven.
Подмены зависимости соседним проектом нет. Параметр release описан в
[документации Maven Compiler Plugin](https://maven.apache.org/plugins-archives/maven-compiler-plugin-3.14.1/).

```sh
mvn compile
java -cp target/classes:$HOME/.m2/repository/io/github/59alex/model-change-set/0.3.0/model-change-set-0.3.0.jar example.catalog.CatalogPreview
```

Команда classpath рассчитана на стандартный локальный репозиторий Maven и POSIX
shell. Пример не обращается к сети и не записывает каталог. Нарушение результата
вызывает AssertionError и неуспешное завершение.

[← English walkthrough](../../docs/0.3.0/catalog.en.md) · [Разбор](../../docs/0.3.0/catalog.ru.md) · [README](../../README.md)

plugins { application }
repositories { mavenLocal(); mavenCentral() }
dependencies { implementation("io.github.59alex:model-change-set:0.3.0") }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(
    providers.gradleProperty("exampleJava").getOrElse(JavaVersion.current().majorVersion).toInt())) }
tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}
application { mainClass.set("example.catalog.CatalogPreview") }

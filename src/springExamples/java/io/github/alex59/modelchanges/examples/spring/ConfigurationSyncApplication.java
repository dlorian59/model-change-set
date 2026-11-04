package io.github.alex59.modelchanges.examples.spring;

import io.github.alex59.modelchanges.ChangeType;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

// Run with ./gradlew runSpringExample; the context uses real typed Spring bean injection.
// Запуск: ./gradlew runSpringExample; контекст использует настоящее типизированное внедрение Spring.
public final class ConfigurationSyncApplication {
    private ConfigurationSyncApplication() { }

    public static void main(String[] args) {
        var oldObservation = Instant.parse("2026-01-01T00:00:00Z");
        var newObservation = oldObservation.plusSeconds(60);
        var current = List.of(
            new WorkerPolicy("mail", 4, Duration.ofSeconds(30), oldObservation),
            new WorkerPolicy("legacy", 1, Duration.ofSeconds(10), oldObservation),
            new WorkerPolicy("audit", 2, Duration.ofSeconds(20), oldObservation));
        var desired = List.of(
            new WorkerPolicy("mail", 8, Duration.ofSeconds(45), newObservation),
            new WorkerPolicy("push", 3, Duration.ofSeconds(15), newObservation),
            new WorkerPolicy("audit", 2, Duration.ofSeconds(20), newObservation));

        try (var context = new AnnotationConfigApplicationContext(PolicyConfiguration.class)) {
            var sync = context.getBean(ConfigurationSync.class);
            var preview = sync.preview(current, desired);
            preview.changes().forEach(change -> {
                System.out.println(change.type() + " " + change.key());
                change.fields().forEach(field -> System.out.println(
                    "  " + field.name() + ": " + field.before() + " -> " + field.after()));
            });
            // Check business changes, ignored audit metadata and unchanged source snapshots.
            // Проверяем бизнес-изменения, игнорирование метаданных аудита и неизменность снимков.
            require(preview.changes().stream().map(change -> change.key()).toList()
                .equals(List.of("legacy", "mail", "push")), "Unexpected rollout candidates");
            require(preview.changes().stream().map(change -> change.type()).toList()
                .equals(List.of(ChangeType.REMOVED, ChangeType.MODIFIED, ChangeType.ADDED)), "Wrong rollout direction");
            require(preview.modified().get(0).fields().stream().map(field -> field.name()).toList()
                .equals(List.of("parallelism", "timeout")), "Changed fields differ from the policy");
            require(current.get(0).parallelism() == 4 && desired.get(0).parallelism() == 8,
                "A preview must not mutate configuration");
            // Once the host safely applies desired values, a fresh matching snapshot needs no rollout.
            // После безопасного применения настроек приложением совпадающий снимок не требует изменений.
            require(sync.preview(desired, desired).isEmpty(), "Converged configuration must have no changes");
            System.out.println("Converged: no changes");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

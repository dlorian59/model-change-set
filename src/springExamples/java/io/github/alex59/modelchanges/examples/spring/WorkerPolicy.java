package io.github.alex59.modelchanges.examples.spring;

import java.time.Duration;
import java.time.Instant;

// Business configuration has a stable key; observedAt is audit metadata, not a policy change.
// У бизнес-конфигурации стабильный ключ; observedAt — метаданные аудита, а не изменение политики.
public record WorkerPolicy(String key, int parallelism, Duration timeout, Instant observedAt) { }

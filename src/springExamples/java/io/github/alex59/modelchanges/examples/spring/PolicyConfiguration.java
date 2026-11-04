package io.github.alex59.modelchanges.examples.spring;

import io.github.alex59.modelchanges.Field;
import io.github.alex59.modelchanges.ModelDiffer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class PolicyConfiguration {
    @Bean
    public ModelDiffer<WorkerPolicy, String> workerPolicyDiffer() {
        // Declare exactly what matters for a rollout; observedAt is deliberately omitted.
        // Явно задаём поля, влияющие на применение настроек; observedAt намеренно исключён.
        return ModelDiffer.of(WorkerPolicy::key, Comparator.naturalOrder(), List.of(
            Field.of("parallelism", WorkerPolicy::parallelism),
            Field.of("timeout", WorkerPolicy::timeout)));
    }

    @Bean
    public ConfigurationSync configurationSync(ModelDiffer<WorkerPolicy, String> workerPolicyDiffer) {
        // Generic types keep this injection specific to worker policies without a registry or casts.
        // Типы ограничивают внедрение настройками обработчиков; реестр и приведения типов не нужны.
        return new ConfigurationSync(workerPolicyDiffer);
    }
}

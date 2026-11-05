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

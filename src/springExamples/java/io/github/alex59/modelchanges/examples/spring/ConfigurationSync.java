package io.github.alex59.modelchanges.examples.spring;

import io.github.alex59.modelchanges.ChangeSet;
import io.github.alex59.modelchanges.ModelDiffer;

import java.util.List;

// The same preview service can serve an approval screen or a deployment command.
// Один сервис предварительного просмотра подходит для экрана согласования и команды развёртывания.
public final class ConfigurationSync {
    private final ModelDiffer<WorkerPolicy, String> differ;

    public ConfigurationSync(ModelDiffer<WorkerPolicy, String> differ) {
        this.differ = differ;
    }

    public ChangeSet<WorkerPolicy, String> preview(List<WorkerPolicy> current, List<WorkerPolicy> desired) {
        // Read both snapshots consistently before calling; this method performs no writes.
        // До вызова прочитайте согласованные снимки; этот метод ничего не записывает.
        return differ.compare(current, desired);
    }
}

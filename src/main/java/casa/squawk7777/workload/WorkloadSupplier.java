package casa.squawk7777.workload;

import java.util.Set;

public interface WorkloadSupplier<T extends WorkloadItem> {
    Set<T> fetchWorkload();
}

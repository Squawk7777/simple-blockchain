package casa.squawk7777;

import java.util.Set;

public interface WorkloadSupplier<T extends WorkloadItem> {
    Set<T> fetchWorkload();
}

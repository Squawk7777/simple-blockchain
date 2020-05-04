package casa.squawk7777.workload;

import casa.squawk7777.Transaction;

import java.util.Set;

public class TransactionUtil {
    private TransactionUtil() {}

    /**
     * Finds max workload ID value from workload Set
     */
    public static Integer maxTransactionId(Set<Transaction> workload) {
        return workload.stream()
                .mapToInt(Transaction::getId)
                .max()
                .orElse(0);
    }
}

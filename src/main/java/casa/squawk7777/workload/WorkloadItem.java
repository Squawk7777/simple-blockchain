package casa.squawk7777.workload;

import java.security.PublicKey;

public interface WorkloadItem extends Comparable<WorkloadItem> {
    Integer getId();

    String getData();

    byte[] getSignature();

    PublicKey getPublicKey();
}

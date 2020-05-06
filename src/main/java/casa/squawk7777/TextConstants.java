package casa.squawk7777;

public class TextConstants {
    private TextConstants() {}

    public static final String HAS_INVALID_ID = "Block has invalid ID";
    public static final String NOT_MEET_COMPLEXITY = "Block hash does not meet the required complexity";
    public static final String NOT_PRESENT_IN_THE_POOL = "At least one of transactions is not present in the pool";
    public static final String HASH_DIFFERS_FROM_CALCULATED = "Block hash differs from calculated";

    public static final String UNABLE_TO_CALCULATE_HASH = "Unable to calculate hash";
    public static final String UNABLE_TO_GENERATE_KEYS = "Unable to generate security keys";
    public static final String SIGNATURE_IS_INVALID = "Signature of this data is invalid!";

    public static final String BLOCKCHAIN_CLOSED = "Blockchain is closed";
    public static final String TRANSACTION_ALREADY_EXIST = "Transaction with such ID is already exist";
    public static final String SENDER_IS_SHORT_ON_FUNDS = "Sender doesn't have enough funds to cary out transaction";

    public static final String CHALLENGE_FINISHED = "Generation aborted. Current challenge is finished by someone else, asking for new...";
}

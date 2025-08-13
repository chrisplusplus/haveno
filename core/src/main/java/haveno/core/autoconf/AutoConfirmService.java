package haveno.core.autoconf;

import java.math.BigInteger;

/**
 * Service used to verify that a blockchain transaction has the expected
 * receiver, amount and number of confirmations.
 */
public interface AutoConfirmService {

    /**
     * Verify that the transaction identified by {@code txId} pays {@code amount}
     * to {@code receiverAddress} and has at least {@code requiredConfirmations} confirmations.
     *
     * @param txId the transaction identifier
     * @param receiverAddress the expected recipient address
     * @param amount expected amount in the chain's smallest unit
     * @param requiredConfirmations minimum confirmations required
     * @return {@code true} if the transaction satisfies all constraints
     * @throws Exception if the underlying RPC call fails
     */
    boolean verify(String txId,
                   String receiverAddress,
                   BigInteger amount,
                   int requiredConfirmations) throws Exception;
}


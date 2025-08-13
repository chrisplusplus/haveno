package haveno.core.autoconf;

/**
 * Auto confirm service for Bitcoin Cash. Functionality is identical to
 * {@link BitcoinAutoConfirmService} as Bitcoin Cash exposes the same JSON-RPC
 * interface as Bitcoin Core.
 */
public class BitcoinCashAutoConfirmService extends BitcoinAutoConfirmService {

    public BitcoinCashAutoConfirmService(String rpcUrl, String rpcUser, String rpcPassword) {
        super(rpcUrl, rpcUser, rpcPassword);
    }
}


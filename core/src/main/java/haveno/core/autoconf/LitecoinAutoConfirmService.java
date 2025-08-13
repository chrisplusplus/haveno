package haveno.core.autoconf;

/**
 * Auto confirm service for Litecoin. Uses the same logic as
 * {@link BitcoinAutoConfirmService}.
 */
public class LitecoinAutoConfirmService extends BitcoinAutoConfirmService {

    public LitecoinAutoConfirmService(String rpcUrl, String rpcUser, String rpcPassword) {
        super(rpcUrl, rpcUser, rpcPassword);
    }
}


package haveno.core.ethereum;

import haveno.core.user.Preferences;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to auto-confirm Ethereum based transfers.
 *
 * <p>This implementation keeps a cache of confirmed transaction hashes in order to
 * detect address or transaction reuse. When a reuse is detected the configured
 * {@link Preferences} instance is notified so the UI can warn the user.</p>
 */
public class EthereumAutoConfirmService {

    /**
     * Simple container for token metadata.
     */
    private static class TokenMetadata {
        final String name;
        final String symbol;

        TokenMetadata(String name, String symbol) {
            this.name = name;
            this.symbol = symbol;
        }
    }

    // Known ERC20 token contracts and their metadata.
    private static final Map<String, TokenMetadata> KNOWN_TOKENS = Map.of(
            // DAI Stablecoin
            "0x6b175474e89094c44da98b954eedeac495271d0f", new TokenMetadata("Dai Stablecoin", "DAI-ERC20"),
            // USD Coin
            "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48", new TokenMetadata("USD Coin", "USDC"),
            // Tether USD
            "0xdac17f958d2ee523a2206206994597c13d831ec7", new TokenMetadata("Tether USD", "USDT"),
            // TrueUSD
            "0x0000000000085d4780b73119b644ae5ecd22b376", new TokenMetadata("TrueUSD", "TUSD")
    );

    private final Set<String> confirmedTxs = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Preferences preferences;

    public EthereumAutoConfirmService(Preferences preferences) {
        this.preferences = preferences;
    }

    /**
     * Verify that a provided contract address exists and matches known token metadata.
     *
     * @param contractAddress the ERC20 contract address
     * @param tokenName       the token's name supplied by the user
     * @param tokenSymbol     the token's symbol supplied by the user
     * @return {@code true} if the contract is recognised and matches the expected metadata
     */
    public boolean verifyContractAddress(String contractAddress, String tokenName, String tokenSymbol) {
        if (contractAddress == null) return false;
        TokenMetadata meta = KNOWN_TOKENS.get(contractAddress.toLowerCase());
        return meta != null && meta.name.equals(tokenName) && meta.symbol.equals(tokenSymbol);
    }

    /**
     * Validate that a transfer amount matches the trade amount and that the required
     * confirmations have been met.
     *
     * @param transferAmount       amount transferred in smallest units
     * @param tradeAmount          amount expected for the trade in smallest units
     * @param confirmations        number of confirmations the transaction has
     * @param requiredConfirmations number of confirmations required
     * @return {@code true} if the amounts match and confirmations are sufficient
     */
    public boolean validateTransaction(BigInteger transferAmount, BigInteger tradeAmount, int confirmations, int requiredConfirmations) {
        return transferAmount != null && transferAmount.equals(tradeAmount) && confirmations >= requiredConfirmations;
    }

    /**
     * Track confirmed transaction hashes and detect reuse. If a transaction hash is
     * seen more than once the user is warned via {@link Preferences}.
     *
     * @param txHash the transaction hash to test
     * @return {@code true} if the transaction hash has been seen before
     */
    public boolean isTxHashReused(String txHash) {
        if (txHash == null) return false;
        if (confirmedTxs.contains(txHash)) {
            preferences.setAddressReuseDetected(true);
            return true;
        }
        confirmedTxs.add(txHash);
        return false;
    }
}


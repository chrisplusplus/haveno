package haveno.core.autoconf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Auto confirm service for Ethereum and ERC20 tokens using JSON-RPC.
 */
public class EthereumAutoConfirmService implements AutoConfirmService {

    private static final String TRANSFER_TOPIC = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

    private final String rpcUrl;

    public EthereumAutoConfirmService(String rpcUrl) {
        this.rpcUrl = rpcUrl;
    }

    @Override
    public boolean verify(String txId, String receiverAddress, BigInteger amount, int requiredConfirmations) throws Exception {
        JsonObject tx = rpc("eth_getTransactionByHash", txId).getAsJsonObject();
        if (tx == null) return false;
        if (!receiverAddress.equalsIgnoreCase(tx.get("to").getAsString())) return false;
        BigInteger value = new BigInteger(tx.get("value").getAsString().substring(2), 16);
        if (!value.equals(amount)) return false;
        BigInteger confirmations = getConfirmations(txId);
        return confirmations.intValue() >= requiredConfirmations;
    }

    /**
     * Verify an ERC20 token transfer.
     */
    public boolean verifyToken(String txId,
                               String receiverAddress,
                               BigInteger amount,
                               int requiredConfirmations,
                               String contractAddress,
                               int expectedDecimals) throws Exception {
        // verify contract exists
        JsonElement code = rpc("eth_getCode", contractAddress, "latest");
        if (code == null || code.getAsString().equals("0x")) return false;

        // verify decimals
        JsonObject callObj = new JsonObject();
        callObj.addProperty("to", contractAddress);
        callObj.addProperty("data", "0x313ce567"); // decimals()
        JsonElement decimalsRes = rpc("eth_call", callObj, "latest");
        if (decimalsRes == null) return false;
        int decimals = new BigInteger(decimalsRes.getAsString().substring(2), 16).intValue();
        if (decimals != expectedDecimals) return false;

        // check transaction receipt logs
        JsonObject receipt = rpc("eth_getTransactionReceipt", txId).getAsJsonObject();
        if (receipt == null) return false;
        BigInteger confirmations = getConfirmationsFromReceipt(receipt);
        if (confirmations.intValue() < requiredConfirmations) return false;
        JsonArray logs = receipt.getAsJsonArray("logs");
        for (JsonElement el : logs) {
            JsonObject log = el.getAsJsonObject();
            if (!contractAddress.equalsIgnoreCase(log.get("address").getAsString())) continue;
            JsonArray topics = log.getAsJsonArray("topics");
            if (topics.size() < 3) continue;
            if (!TRANSFER_TOPIC.equalsIgnoreCase(topics.get(0).getAsString())) continue;
            String toTopic = topics.get(2).getAsString();
            String to = "0x" + toTopic.substring(toTopic.length() - 40);
            if (!to.equalsIgnoreCase(receiverAddress)) continue;
            BigInteger value = new BigInteger(log.get("data").getAsString().substring(2), 16);
            if (value.equals(amount)) return true;
        }
        return false;
    }

    private BigInteger getConfirmations(String txId) throws Exception {
        JsonObject receipt = rpc("eth_getTransactionReceipt", txId).getAsJsonObject();
        return getConfirmationsFromReceipt(receipt);
    }

    private BigInteger getConfirmationsFromReceipt(JsonObject receipt) throws Exception {
        if (receipt == null || receipt.get("blockNumber").isJsonNull()) return BigInteger.ZERO;
        BigInteger txBlock = new BigInteger(receipt.get("blockNumber").getAsString().substring(2), 16);
        JsonElement latestBlockRes = rpc("eth_blockNumber");
        BigInteger latest = new BigInteger(latestBlockRes.getAsString().substring(2), 16);
        return latest.subtract(txBlock).add(BigInteger.ONE);
    }

    private JsonElement rpc(String method, Object... params) throws Exception {
        JsonObject req = new JsonObject();
        req.addProperty("jsonrpc", "2.0");
        req.addProperty("id", 1);
        req.addProperty("method", method);
        JsonArray arr = new JsonArray();
        for (Object p : params) {
            if (p instanceof String) arr.add((String) p);
            else if (p instanceof JsonElement) arr.add((JsonElement) p);
            else if (p instanceof JsonObject) arr.add((JsonObject) p);
        }
        req.add("params", arr);

        URL url = new URL(rpcUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(req.toString().getBytes(StandardCharsets.UTF_8));
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            JsonObject resp = new JsonParser().parse(sb.toString()).getAsJsonObject();
            return resp.get("result");
        }
    }
}


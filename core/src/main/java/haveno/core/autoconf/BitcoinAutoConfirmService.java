package haveno.core.autoconf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Implementation of {@link AutoConfirmService} for Bitcoin like chains using the
 * JSON-RPC interface provided by Bitcoin Core and derivatives.
 */
public class BitcoinAutoConfirmService implements AutoConfirmService {

    private final String rpcUrl;
    private final String rpcUser;
    private final String rpcPassword;

    public BitcoinAutoConfirmService(String rpcUrl, String rpcUser, String rpcPassword) {
        this.rpcUrl = rpcUrl;
        this.rpcUser = rpcUser;
        this.rpcPassword = rpcPassword;
    }

    @Override
    public boolean verify(String txId, String receiverAddress, BigInteger amount, int requiredConfirmations) throws Exception {
        JsonObject tx = rpc("gettransaction", txId).getAsJsonObject();
        if (tx == null) return false;
        JsonElement confEl = tx.get("confirmations");
        if (confEl == null || confEl.getAsInt() < requiredConfirmations) return false;
        JsonArray details = tx.getAsJsonArray("details");
        if (details == null) return false;
        for (JsonElement el : details) {
            JsonObject det = el.getAsJsonObject();
            if (receiverAddress.equals(det.get("address").getAsString())) {
                BigDecimal value = det.get("amount").getAsBigDecimal();
                BigInteger satoshis = value.movePointRight(8).toBigInteger();
                if (satoshis.compareTo(amount) == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    protected JsonObject rpc(String method, String param) throws Exception {
        JsonArray params = new JsonArray();
        if (param != null) params.add(param);
        JsonObject req = new JsonObject();
        req.addProperty("jsonrpc", "1.0");
        req.addProperty("id", "autoconf");
        req.addProperty("method", method);
        req.add("params", params);

        URL url = new URL(rpcUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        String auth = Base64.getEncoder().encodeToString((rpcUser + ":" + rpcPassword).getBytes(StandardCharsets.UTF_8));
        conn.setRequestProperty("Authorization", "Basic " + auth);
        conn.setRequestProperty("Content-Type", "application/json");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(req.toString().getBytes(StandardCharsets.UTF_8));
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            JsonObject resp = new JsonParser().parse(sb.toString()).getAsJsonObject();
            return resp.getAsJsonObject("result");
        }
    }
}


package nl.utwente.atelier.pmd.server;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import nl.utwente.atelier.pmd.util.Hex;

public class WebhookHandler {
    private HttpClient client;
    private String webhookSecret;

    public WebhookHandler(HttpClient client, String webhookSecret) {
        this.client = client;
        this.webhookSecret = webhookSecret;
    }

    private class InvalidWebhookRequest extends Throwable {
        public InvalidWebhookRequest(String reason) {
            super("Invalid request. " + reason);
        }
    }

    private class CryptoException extends Exception {
        public CryptoException(Exception ex) {
            super(ex);
        }
    }

    private void checkUserAgent(HttpServletRequest request) throws InvalidWebhookRequest {
        var userAgent = request.getHeader("User-Agent");
        if (userAgent == null || !userAgent.equals("Atelier")) {
            throw new InvalidWebhookRequest("Unknown User-Agent.");
        }
    }

    private void checkSignature(HttpServletRequest request, byte[] body) throws InvalidWebhookRequest, CryptoException {
        var signature = request.getHeader("X-Atelier-Signature");
        if (signature == null) {
            throw new InvalidWebhookRequest("No signature provided.");
        }
        
        try {
            var secret = webhookSecret.getBytes();
            var key = new SecretKeySpec(secret, "HmacSHA1");
            var hmac = Mac.getInstance("HmacSHA1");
            hmac.init(key);

            var rawSignature = hmac.doFinal(body);
            var computedSignature = Hex.bytesToHex(rawSignature);

            if (!computedSignature.equals(signature)) {
                throw new InvalidWebhookRequest("Invalid signature");
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new CryptoException(e);
        }
    }

    public void handleWebhook(HttpServletRequest request, HttpServletResponse response) {
        try {
            // I'm sorry.
            // The inner catch handler may throw an IOException, which should be handled in the
            // same way as any thrown in try-block. The alternative would be another try-catch
            // inside the catch, with duplicated error-handling code.
            try {
                checkUserAgent(request);
                var rawBody = request.getInputStream().readAllBytes();
                checkSignature(request, rawBody);
                var body = new String(rawBody, "UTF-8");
                var json = JsonParser.parseString(body);
                var event = json.getAsJsonObject().get("event").getAsString();
                var payload = json.getAsJsonObject().get("payload").getAsJsonObject();
                switch (event) {
                    case "submission.file":
                        handleFileSubmission(payload);
                        break;
                }
            } catch (InvalidWebhookRequest e) {
                response.setStatus(400);
                var writer = response.getWriter();
                writer.println(e.getMessage());
                writer.flush();
                writer.close();
            }
        } catch (IOException | CryptoException e) {
            response.setStatus(500);
            e.printStackTrace();
        }
    }

    private void handleFileSubmission(JsonObject file) {
        var name = file.get("name").getAsString();
        // We only handle processing files, so restrict to those
        if (name.endsWith(".pde")) {
            var fileID = file.get("ID").getAsString();

            // TODO: 
            // - Do authentication dance
            // - Get the file body through API
            // - Process the file with PMD
            // - Submit comments back to Atelier
            var fileRequest = HttpRequest.newBuilder().GET()
                    .uri(URI.create("https://linux571.ewi.utwente.nl/api/file/" + fileID + "/body"))
                    .setHeader("Authorization", "Bearer token").build();
    
            client.sendAsync(fileRequest, HttpResponse.BodyHandlers.ofInputStream())
            .thenAccept(res -> {
                res.body();
            });
        }
    }
}
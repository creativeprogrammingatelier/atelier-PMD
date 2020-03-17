package nl.utwente.atelier.pmd.server;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.ThreadSafeReportListener;
import net.sourceforge.pmd.stat.Metric;
import nl.utwente.atelier.api.Authentication;
import nl.utwente.atelier.exceptions.CryptoException;
import nl.utwente.atelier.pmd.PMDFileProcessor;

public class WebhookHandler {
    private HttpClient client;
    private Authentication auth;
    private String webhookSecret;
    private PMDFileProcessor pmd = new PMDFileProcessor();

    public WebhookHandler(HttpClient client, Authentication auth, String webhookSecret) {
        this.client = client;
        this.auth = auth;
        this.webhookSecret = webhookSecret;
    }

    private class InvalidWebhookRequest extends Throwable {
        public InvalidWebhookRequest(String reason) {
            super("Invalid request. " + reason);
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
            var computedSignature = Base64.getEncoder().encodeToString(rawSignature);

            if (!computedSignature.equals(signature)) {
                throw new InvalidWebhookRequest("Invalid signature.");
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new CryptoException(e);
        }
    }

    public void handleWebhook(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("Received new webhook request.");
        try {
            // I'm sorry.
            // The inner catch handler may throw an IOException, which should be handled in
            // the same way as any thrown in try-block. The alternative would be another
            // try-catch inside the catch, with duplicated error-handling code.
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
                System.out.println(e.getMessage());
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

    private class CommentCreatingReportListener implements ThreadSafeReportListener {
        private String fileID;

        public CommentCreatingReportListener(String fileID) {
            this.fileID = fileID;
        }

        @Override
        public void ruleViolationAdded(RuleViolation ruleViolation) {
            System.out.println("Violation in " + ruleViolation.getFilename() + ":" + ruleViolation.getBeginLine() + ": " + ruleViolation.getDescription());
        }

        @Override
        public void metricAdded(Metric metric) { }
    }

    private void handleFileSubmission(JsonObject file) throws CryptoException, IOException {
        var fileName = file.get("name").getAsString();
        // We only handle processing files, so restrict to those
        if (fileName.endsWith(".pde")) {
            var fileID = file.get("ID").getAsString();
            System.out.printf("Processing %s (ID: %s)%n", fileName, fileID);

            var token = auth.getCurrentToken();
            if (token != null) {
                var fileRequest = HttpRequest.newBuilder().GET()
                    .uri(URI.create("http://192.168.10.9:5000/api/file/" + fileID + "/body"))
                    .setHeader("Authorization", "Bearer " + token).build();

                try {
                    var res = client.send(fileRequest, BodyHandlers.ofInputStream());
                    if (res.statusCode() < 400) {
                        pmd.ProcessFile(res.body(), fileName, new CommentCreatingReportListener(fileID));
                    } else {
                        System.out.printf("Request for file %s returned status %d.", fileID, res.statusCode());
                    }
                } catch (InterruptedException e) {
                    System.out.printf("Request for file (ID: %s) failed.%n", fileID);
                }
            }
        }
    }
}
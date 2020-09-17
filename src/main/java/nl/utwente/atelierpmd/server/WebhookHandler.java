package nl.utwente.atelierpmd.server;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import nl.utwente.atelier.api.AtelierAPI;
import nl.utwente.processing.pmd.PMDException;

import nl.utwente.atelier.exceptions.CryptoException;
import nl.utwente.atelier.pmd.AtelierPMDRenderer;
import nl.utwente.processing.ProcessingFile;
import nl.utwente.processing.ProcessingProject;
import nl.utwente.processing.pmd.PMDRunner;

/** Handler for Webhook requests. It checks if the request is valid and handles supported events. */
public class WebhookHandler {
    private final String webhookSecret;
    private final AtelierAPI api;
    private final PMDRunner pmd = new PMDRunner();

    public WebhookHandler(Configuration config, AtelierAPI api) {
        this.webhookSecret = config.getWebhookSecret();
        this.api = api;
    }

    /** Indicates that the request is not valid due to the given reason */
    private class InvalidWebhookRequest extends Throwable {
        public InvalidWebhookRequest(String reason) {
            super("Invalid request. " + reason);
        }
    }

    /** Check that the request has the correct User-Agent header */
    private void checkUserAgent(HttpServletRequest request) throws InvalidWebhookRequest {
        var userAgent = request.getHeader("User-Agent");
        if (userAgent == null || !userAgent.equals("Atelier")) {
            throw new InvalidWebhookRequest("Unknown User-Agent.");
        }
    }

    /** Check that the request has the correct signature, corresponding to the WebhookSecret */
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

    /** Handle an incoming Webhook request */
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
                    case "submission":
                        handleSubmission(payload);
                        break;
                    case "submission.file":
                        handleFileSubmission(payload);
                        break;
                }
                response.setStatus(200);
            } catch (InvalidWebhookRequest e) {
                System.out.println(e.getMessage());
                response.setStatus(400);
                var writer = response.getWriter();
                writer.println(e.getMessage());
                writer.flush();
                writer.close();
            }
        } catch (IOException | CryptoException | PMDException e) {
            response.setStatus(500);
            e.printStackTrace();
        }
    }

    /** Handle events of type 'submission' */
    private void handleSubmission(JsonObject submission) throws CryptoException, IOException, PMDException {
        var submissionID = submission.get("ID").getAsString();
        System.out.printf("Handling submission %s%n", submissionID);
        try {
            var files = StreamSupport.stream(submission.get("files").getAsJsonArray().spliterator(), true)
                .map(file -> file.getAsJsonObject())
                .filter(file -> file.get("name").getAsString().endsWith(".pde"))
                .map(file -> {
                    try {
                        var fileName = file.get("name").getAsString();
                        var fileID = file.get("ID").getAsString();
                        var res = api.getFile(fileID);
                        if (res.getStatusLine().getStatusCode() < 400) {
                            var fileContent = new String(res.getEntity().getContent().readAllBytes());
                            return new ProcessingFile(fileID, fileName, fileContent);
                        } else {
                            var message = String.format("Request for file %s returned status %d.", fileID, res.getStatusLine().getStatusCode());
                            System.out.printf(message);
                            throw new IOException(message);
                        }
                    } catch (IOException | CryptoException ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .collect(Collectors.toList());
            var project = new ProcessingProject(files);
            var renderer = new AtelierPMDRenderer(submissionID, project, api);
            pmd.Run(project, renderer);
        } catch (RuntimeException ex) {

        }
    }

    /** Handle events of type 'submission.file' */
    private void handleFileSubmission(JsonObject file) throws CryptoException, IOException, PMDException {
        var fileName = file.get("name").getAsString();
        // We only handle processing files, so restrict to those
        if (fileName.endsWith(".pde")) {
            var fileID = file.get("ID").getAsString();
            var submissionID = file.get("references").getAsJsonObject().get("submissionID").getAsString();
            System.out.printf("Processing %s (ID: %s)%n", fileName, fileID);

            var res = api.getFile(fileID);
            if (res.getStatusLine().getStatusCode() < 400) {
                var fileContent = new String(res.getEntity().getContent().readAllBytes());
                var files = Collections.singletonList(new ProcessingFile(fileID, fileName, fileContent));
                var project = new ProcessingProject(files);
                var renderer = new AtelierPMDRenderer(submissionID, project, api);
                pmd.Run(project, renderer);
            } else {
                System.out.printf("Request for file %s returned status %d.", fileID, res.getStatusLine().getStatusCode());
            }
        }
    }
}
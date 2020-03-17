package nl.utwente.atelier.pmd.server;

import java.io.IOException;

import javax.servlet.http.*;

import nl.utwente.atelier.api.Authentication;
import nl.utwente.atelier.exceptions.CryptoException;
import org.apache.http.impl.client.HttpClients;

public class Webhook extends HttpServlet {
    private WebhookHandler handler;

    public Webhook() throws IOException, CryptoException {
        var httpClient = HttpClients.createDefault();
        var auth = new Authentication("XqY+FynTRQKGT94LIsCGbA", "D:\\Arthur\\GitSource\\UTwente\\MOD11\\atelier-pmd\\keys\\jwtRS256.key.pub", "D:\\Arthur\\GitSource\\UTwente\\MOD11\\atelier-pmd\\keys\\jwtRS256.key", httpClient);
        this.handler = new WebhookHandler(httpClient, auth, "webhookSecret");
        System.out.println("Webhook started.");
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handler.handleWebhook(request, response);
    }
}
package nl.utwente.atelier.pmd.server;

import java.io.*;
import java.net.http.HttpClient;

import javax.servlet.http.*;


public class Webhook extends HttpServlet {
    WebhookHandler handler = new WebhookHandler(HttpClient.newHttpClient(), "webhookSecret");

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handler.handleWebhook(request, response);
    }
}
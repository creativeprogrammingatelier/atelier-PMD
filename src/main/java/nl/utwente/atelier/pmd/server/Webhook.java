package nl.utwente.atelier.pmd.server;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.http.*;

import nl.utwente.atelier.api.AtelierAPI;
import nl.utwente.atelier.api.Authentication;
import nl.utwente.atelier.exceptions.ConfigurationException;
import nl.utwente.atelier.exceptions.CryptoException;

import org.apache.http.impl.client.HttpClients;

public class Webhook extends HttpServlet {
    private WebhookHandler handler;

    public Webhook() throws IOException, CryptoException, ConfigurationException, URISyntaxException {
        var httpClient = HttpClients.createDefault();
        var config = Configuration.readFromFile();
        var auth = new Authentication(config, httpClient);
        var api = new AtelierAPI(config, auth, httpClient);
        this.handler = new WebhookHandler(config, api);
        System.out.println("Webhook started.");
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handler.handleWebhook(request, response);
    }
}
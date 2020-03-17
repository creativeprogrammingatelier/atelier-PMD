package nl.utwente.atelier.api;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.google.gson.JsonParser;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import nl.utwente.atelier.exceptions.CryptoException;
import nl.utwente.atelier.pmd.server.Configuration;

public class Authentication {
    private String userID;
    private String atelierHost;
    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private HttpClient client;

    private String currentToken = null;
    private Instant currentTokenExp = null;

    public Authentication(Configuration config, HttpClient client)
            throws IOException, CryptoException {
        this.userID = config.getAtelierPluginUserID();
        this.atelierHost = config.getAtelierHost();
        this.publicKey = (RSAPublicKey) config.getPublicKey();
        this.privateKey = (RSAPrivateKey) config.getPrivateKey();
        this.client = client;
    }

    private String issueToken() throws CryptoException {
        try {
            Algorithm algorithm = Algorithm.RSA256(publicKey, privateKey);
            return JWT.create().withIssuer(userID).sign(algorithm);
        } catch (JWTCreationException e) {
            throw new CryptoException(e);
        }
    }

    public synchronized String getCurrentToken() throws CryptoException, IOException {
        if (currentToken == null || currentTokenExp == null || currentTokenExp.isBefore(Instant.now().plusSeconds(15))) {
            System.out.println("Requesting new authentication token.");
            var token = issueToken();
            var authRequest = new HttpGet(atelierHost + "/api/auth/token");
            authRequest.addHeader("Authorization", "Bearer " + token);
            try {
                var res = client.execute(authRequest);
                if (res.getStatusLine().getStatusCode() == 200) {
                    var resToken = JsonParser.parseReader(new InputStreamReader(res.getEntity().getContent()))
                        .getAsJsonObject()
                        .get("token")
                        .getAsString();
                    currentToken = resToken;
                    currentTokenExp = JWT.decode(resToken).getExpiresAt().toInstant();
                } else {
                    System.out.println("Request was unsuccesful, got status " + res.getStatusLine().getStatusCode());
                }
            } catch (NullPointerException e) {
                System.out.println("Got null when trying to read token.");
            }
        }
        return currentToken;
    }
}
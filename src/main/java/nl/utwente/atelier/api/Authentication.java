package nl.utwente.atelier.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.google.gson.JsonParser;

import nl.utwente.atelier.api.utils.PemUtils;
import nl.utwente.atelier.exceptions.CryptoException;

public class Authentication {
    private String userID;
    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private HttpClient client;

    private String currentToken = null;
    private Instant currentTokenExp = null;

    public Authentication(String userID, String publicKeyPath, String privateKeyPath, HttpClient client)
            throws IOException, CryptoException {
        this.userID = userID;
        var keyPair = PemUtils.getKeys(Path.of(publicKeyPath), Path.of(privateKeyPath), "RSA");
        this.publicKey = (RSAPublicKey) keyPair.getPublic();
        this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
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
            var authRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("http://localhost:5000/api/auth/token"))
                .header("Authorization", "Bearer " + token)
                .build();
            try {
                var res = client.send(authRequest, BodyHandlers.ofString());
                if (res.statusCode() == 200) {
                    var resToken = JsonParser.parseString(res.body())
                        .getAsJsonObject()
                        .get("token")
                        .getAsString();
                    currentToken = resToken;
                    currentTokenExp = JWT.decode(resToken).getExpiresAt().toInstant();
                } else {
                    System.out.println("Request was unsuccesful, got status " + res.statusCode() + " with body: " + res.body());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                System.out.println("Got null when trying to read token.");
            }
        }
        return currentToken;
    }

    public static void main(String[] args) {
        try {
            var httpClient = HttpClient.newHttpClient();
            var auth = new Authentication("XqY+FynTRQKGT94LIsCGbA", "D:\\Arthur\\GitSource\\UTwente\\MOD11\\atelier-pmd\\keys\\jwtRS256.key.pub", "D:\\Arthur\\GitSource\\UTwente\\MOD11\\atelier-pmd\\keys\\jwtRS256.key", httpClient);
            System.out.println("Got token: " + auth.getCurrentToken());
        } catch (CryptoException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
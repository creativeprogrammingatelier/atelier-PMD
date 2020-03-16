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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private AtomicBoolean fetching = new AtomicBoolean(false);
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

    public synchronized CompletableFuture<String> getCurrentToken() throws CryptoException {
        while (fetching.get()) {
            try {
                synchronized(fetching) {
                    fetching.wait();
                }
            } catch (InterruptedException e) {
                Thread.yield();
            }
        }

        if (currentToken != null && currentTokenExp != null && currentTokenExp.isAfter(Instant.now().plusSeconds(30))) {
            System.out.println("Returning existing authentication token.");
            var result = new CompletableFuture<String>();
            result.complete(currentToken);
            return result;
        } else {
            fetching.set(true);
            System.out.println("Requesting new authentication token.");
            var token = issueToken();
            var authRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("http://localhost:5000/api/auth/token"))
                .header("Authorization", "Bearer " + token)
                .build();
            return client
                .sendAsync(authRequest, BodyHandlers.ofString())
                .thenApply(res -> {
                    var resToken = JsonParser.parseString(res.body())
                        .getAsJsonObject()
                        .get("token")
                        .getAsString();
                    currentToken = resToken;
                    currentTokenExp = JWT.decode(resToken).getExpiresAt().toInstant();
                    return resToken; })
                .handle((res, e) -> {
                    fetching.set(false);
                    synchronized (fetching) {
                        fetching.notifyAll();
                    }
                    
                    return res; });
        }
    }
}
package nl.utwente.atelier.api;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.google.gson.JsonParser;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

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
            var authRequest = new HttpGet("http://localhost:5000/api/auth/token");
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
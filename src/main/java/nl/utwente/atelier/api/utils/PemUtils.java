package nl.utwente.atelier.api.utils;

import nl.utwente.atelier.exceptions.CryptoException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.stream.Collectors;

public class PemUtils {
    private static final String PUBLIC_START = "-----BEGIN CERTIFICATE-----";
    private static final String PUBLIC_END = "-----END CERTIFICATE-----";
    private static final String PRIVATE_START = "-----BEGIN PRIVATE KEY-----";
    private static final String PRIVATE_END = "-----END PRIVATE KEY-----";

    private static KeyPair generateKeyPair(String algorithm) throws CryptoException {
        try {
            var gen = KeyPairGenerator.getInstance(algorithm);
            return gen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        }
    }

    private static void writeKey(Path path, String start, Key key, String end) throws IOException {
        var keyText = new String(Base64.getEncoder().encode(key.getEncoded()));
        var fileBuilder = new StringBuilder();
        fileBuilder.append(start);
        fileBuilder.append(System.lineSeparator());
        for (int i = 0; i < keyText.length(); i += 64) {
            fileBuilder.append(keyText.substring(i, i + 64 < keyText.length() ? i + 64 : keyText.length()));
            fileBuilder.append(System.lineSeparator());
        }
        fileBuilder.append(end);
        fileBuilder.append(System.lineSeparator());
        Files.createDirectories(path.getParent());
        Files.writeString(path, fileBuilder.toString());
    }

    private static void writePublicKey(Path path, PublicKey key) throws IOException {
        writeKey(path, PUBLIC_START, key, PUBLIC_END);
    }

    private static void writePrivateKey(Path path, PrivateKey key) throws IOException {
        writeKey(path, PRIVATE_START, key, PRIVATE_END);
    }

    private static byte[] readKeyBytes(Path path, String start, String end) throws IOException {
        var content = Files.lines(path)
            .map(line -> line.trim())
            .collect(Collectors.joining())
            .replace(start, "")
            .replace(end, "")
            .trim();
        return Base64.getDecoder().decode(content);
    }

    private static PublicKey readPublicKey(Path path, KeyFactory kf) throws IOException, CryptoException {
        var bytes = readKeyBytes(path, PUBLIC_START, PUBLIC_END);
        try {
            return kf.generatePublic(new X509EncodedKeySpec(bytes));
        } catch (InvalidKeySpecException e) {
            throw new CryptoException(e);
        }
    }
    private static PrivateKey readPrivateKey(Path path, KeyFactory kf) throws IOException, CryptoException {
        var bytes = readKeyBytes(path, PRIVATE_START, PRIVATE_END);
        try {
            return kf.generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (InvalidKeySpecException e) {
            throw new CryptoException(e);
        }
    }

    public static KeyPair getKeys(Path publicPath, Path privatePath, String algorithm) throws CryptoException, IOException {
        if (!Files.exists(publicPath) || !Files.exists(privatePath)) {
            System.out.println("Generating new RSA key pair");
            var keypair = generateKeyPair(algorithm);
            writePublicKey(publicPath, keypair.getPublic());
            writePrivateKey(privatePath, keypair.getPrivate());
            return keypair;
        } else {
            System.out.println("Reading RSA keys from disk");
            try {
                KeyFactory kf = KeyFactory.getInstance(algorithm);
                return new KeyPair(readPublicKey(publicPath, kf), readPrivateKey(privatePath, kf));
            } catch (NoSuchAlgorithmException e) {
                throw new CryptoException(e);
            }
        }
    }
}
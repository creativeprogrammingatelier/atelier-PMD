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
import java.util.stream.Stream;

/** Helper class to deal with public/private keys and certificates */
public class PemUtils {
    private static final String PUBLIC_START = "-----BEGIN CERTIFICATE-----";
    private static final String PUBLIC_END = "-----END CERTIFICATE-----";
    private static final String PRIVATE_START = "-----BEGIN PRIVATE KEY-----";
    private static final String PRIVATE_END = "-----END PRIVATE KEY-----";

    /** Generate a new KeyPair, where algorithm is one of the valid algorithms for Java's KeyPairGenerator */
    public static KeyPair generateKeyPair(String algorithm) throws CryptoException {
        try {
            var gen = KeyPairGenerator.getInstance(algorithm);
            return gen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        }
    }

    /** Write a KeyPair out to disk, on the specified file paths */
    public static void writeKeyPair(KeyPair keyPair, Path publicPath, Path privatePath) throws IOException {
        writeKey(publicPath, PUBLIC_START, keyPair.getPublic(), PUBLIC_END);
        writeKey(privatePath, PRIVATE_START, keyPair.getPrivate(), PRIVATE_END);
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

    private static byte[] readKeyBytes(Stream<String> lines, String start, String end) {
        var content = lines
            .map(line -> line.trim())
            .collect(Collectors.joining())
            .replace(start, "")
            .replace(end, "")
            .trim();
        return Base64.getDecoder().decode(content);
    }

    private static byte[] readKeyBytes(String keyData, String start, String end) {
        return readKeyBytes(keyData.lines(), start, end);
    }

    private static byte[] readKeyBytes(Path path, String start, String end) throws IOException {
        return readKeyBytes(Files.lines(path), start, end);
    }

    private static PublicKey toPublicKey(byte[] keyBytes, KeyFactory kf) throws CryptoException {
        try {
            return kf.generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (InvalidKeySpecException e) {
            throw new CryptoException(e);
        }
    }
    private static PrivateKey toPrivateKey(byte[] keyBytes, KeyFactory kf) throws CryptoException {
        try {
            return kf.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (InvalidKeySpecException e) {
            throw new CryptoException(e);
        }
    }

    /** Create a KeyPair from the binary contents of the keys */
    private static KeyPair getKeyPair(byte[] publicKey, byte[] privateKey, String algorithm) throws CryptoException {
        try {
            KeyFactory kf = KeyFactory.getInstance(algorithm);
            return new KeyPair(toPublicKey(publicKey, kf), toPrivateKey(privateKey, kf));
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        }
    }

    /** Create a KeyPair from the string content of keyfiles */
    public static KeyPair getKeyPair(String publicKey, String privateKey, String algorithm) throws CryptoException {
        var publicBytes = readKeyBytes(publicKey, PUBLIC_START, PUBLIC_END);
        var privateBytes = readKeyBytes(privateKey, PRIVATE_START, PRIVATE_END);
        return getKeyPair(publicBytes, privateBytes, algorithm);
    }

    /** Create a KeyPair from two keyfiles */
    public static KeyPair getKeyPair(Path publicPath, Path privatePath, String algorithm) throws CryptoException, IOException {
        var publicBytes = readKeyBytes(publicPath, PUBLIC_START, PUBLIC_END);
        var privateBytes = readKeyBytes(privatePath, PRIVATE_START, PRIVATE_END);
        return getKeyPair(publicBytes, privateBytes, algorithm);
    }
}
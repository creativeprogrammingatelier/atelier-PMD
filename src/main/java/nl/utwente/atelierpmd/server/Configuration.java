package nl.utwente.atelierpmd.server;

import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import nl.utwente.atelier.api.utils.PemUtils;
import nl.utwente.atelier.exceptions.ConfigurationException;
import nl.utwente.atelier.exceptions.CryptoException;

/** Configuration for Atelier-PMD, contains all environment-dependend settings */
public class Configuration {
    private static final String ENV = "ENV::";
    private static final String FILE = "FILE::";
    private static final String ALGORITHM = "RSA";

    private final String atelierHost;
    private final String atelierPluginUserID;
    private final String webhookSecret;
    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    /**
     * Create a new configuration
     * @param atelierHost the URL for the connected Atelier instance, without a trailing /
     * @param atelierPluginUserID the userID of the plugin within Atelier
     * @param webhookSecret a secret that Atelier uses to sign the webhook requests
     * @param publicKey the public key of this application, used for initial authentication
     * @param privateKey the private key corresponding with the public key
     */
    private Configuration(String atelierHost, String atelierPluginUserID, String webhookSecret, PublicKey publicKey,
            PrivateKey privateKey) {
        this.atelierHost = atelierHost;
        this.atelierPluginUserID = atelierPluginUserID;
        this.webhookSecret = webhookSecret;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    private static String getProp(String value, String field) throws ConfigurationException {
        if (value == null)
            throw new ConfigurationException("Field " + field + " may not be null.");
        if (value.startsWith(ENV)) {
            return getProp(System.getenv(value.substring(ENV.length())), field);
        } else if (value.startsWith(FILE)) {
            try {
                return getProp(Files.readString(Path.of(value.substring(FILE.length()))), field);
            } catch (IOException e) {
                throw new ConfigurationException("Cannot read file for field " + field, e);
            }
        } else {
            return value;
        }
    }

    private static String getJsonField(JsonObject json, String field) throws ConfigurationException {
        var elem = json.get(field);
        if (elem == null)
            throw new ConfigurationException("Field " + field + " may not be null.");
        var value = elem.getAsString();
        if (value == null)
            throw new ConfigurationException("Expected field " + field + " to be of type string.");
        return value;
    }

    private static String getJsonProp(JsonObject json, String field) throws ConfigurationException {
        var value = getJsonField(json, field);
        return getProp(value, field);
    }

    /** 
     * Read the configuration JSON file from the file location specified by the 
     * ATELIER_PMD_CONFIG environment variable 
     */
    public static Configuration readFromFile()
            throws ConfigurationException, CryptoException, IOException, URISyntaxException {
        var env = System.getenv("ATELIER_PMD_CONFIG");
        if (env == null) 
            throw new ConfigurationException("No configuration file set. Please set the ATELIER_PMD_CONFIG " +
                "environment variable to the path of your configuration file.");
        return readFromFile(Path.of(env));
    }

    /** Read the configuration JSON file from the specified file location */
    public static Configuration readFromFile(Path file) throws ConfigurationException, CryptoException, IOException {
        var config = JsonParser.parseReader(new FileReader(file.toFile())).getAsJsonObject();

        KeyPair keyPair;
        try {
            keyPair = PemUtils.getKeyPair(getJsonProp(config, "publicKey"), getJsonProp(config, "privateKey"), ALGORITHM);
        } catch (ConfigurationException e) {
            var pubField = getJsonField(config, "publicKey");
            var privField = getJsonField(config, "privateKey");
            if (pubField.startsWith(FILE) && privField.startsWith(FILE)) {
                keyPair = PemUtils.generateKeyPair(ALGORITHM);
                PemUtils.writeKeyPair(keyPair, 
                    Path.of(pubField.substring(FILE.length())), 
                    Path.of(privField.substring(FILE.length())));
            } else {
                throw new ConfigurationException("No RSA keys could be found, please set the publicKey and privateKey " +
                    "properties to a '" + FILE + "' value if you want to generate them.");
            }
        }

        return new Configuration(
            getJsonProp(config, "atelierHost"), 
            getJsonProp(config, "atelierPluginUserID"), 
            getJsonProp(config, "webhookSecret"), 
            keyPair.getPublic(),
            keyPair.getPrivate()
        );
    }

    /** The URL of the connected Atelier instance */
    public String getAtelierHost() {
        return atelierHost;
    }

    /** The user ID of the registered plugin in Atelier */
    public String getAtelierPluginUserID() {
        return atelierPluginUserID;
    }

    /** The secret Atelier uses to sign Webhook requests */
    public String getWebhookSecret() {
        return webhookSecret;
    }

    /** The public key of this application, used for initial authentication */
    public PublicKey getPublicKey() {
        return publicKey;
    }

    /** The private key corresponding to our public key */
    public PrivateKey getPrivateKey() {
        return privateKey;
    }
}
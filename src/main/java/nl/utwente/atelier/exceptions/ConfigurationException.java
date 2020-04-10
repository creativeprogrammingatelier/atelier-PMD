package nl.utwente.atelier.exceptions;

/** Indicates that the configuration is not valid */
public class ConfigurationException extends Exception {
    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Exception e) {
        super(message, e);
    }
}
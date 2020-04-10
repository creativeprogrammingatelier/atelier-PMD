package nl.utwente.atelier.exceptions;

/** Something went wrong with cryptographic things for authentication */
public class CryptoException extends Exception {
    public CryptoException(Exception ex) {
        super(ex);
    }
}
package nl.utwente.atelier.exceptions;

public class CryptoException extends Exception {
    public CryptoException(Exception ex) {
        super(ex);
    }
}
package nl.utwente.atelier.exceptions;

/** Something went wrong while running PMD */
public class PMDException extends Exception {
    public PMDException(Exception ex) {
        super(ex);
    }
}

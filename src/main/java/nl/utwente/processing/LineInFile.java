package nl.utwente.processing;

import nl.utwente.atelier.pmd.PMDFile;

/** A line in a file. This exists, because Java. */
public class LineInFile {
    private final int line;
    private final PMDFile file;

    public LineInFile(final int line, final PMDFile file) {
        this.line = line;
        this.file = file;
    }

    public int getLine() {
        return line;
    }

    public PMDFile getFile() {
        return file;
    }
}
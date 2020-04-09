package nl.utwente.atelier.pmd;

public class PMDFile {
    private final String id;
    private final String content;

    public PMDFile(final String id, final String content) {
        this.id = id;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }
}
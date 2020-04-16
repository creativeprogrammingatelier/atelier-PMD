package nl.utwente.atelier.pmd;

/** Helper class to store information about a file */
public class PMDFile {
    private final String id;
    private final String name;
    private final String content;

    public PMDFile(final String id, final String name, final String content) {
        this.id = id;
        this.name = name;
        this.content = content;
    }
    
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }
}
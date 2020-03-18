package nl.utwente.atelier.pmd;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import org.apache.http.client.HttpClient;

import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.renderers.AbstractIncrementingRenderer;

import nl.utwente.atelier.api.Authentication;
import nl.utwente.atelier.pmd.server.Configuration;

public class AtelierPMDRenderer extends AbstractIncrementingRenderer {

    private final String fileID;
    private final Authentication auth;
    private final Configuration config;
    private final HttpClient client;

    public AtelierPMDRenderer(String fileID, Authentication auth, Configuration config, HttpClient client) {
        super("Atelier-" + fileID, "Uploads comments directly to Atelier, on file " + fileID);
        this.fileID = fileID;
        this.auth = auth;
        this.config = config;
        this.client = client;
    }

    private class NoWriter extends Writer {
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException { }

        @Override
        public void flush() throws IOException { }

        @Override
        public void close() throws IOException { }
    }

    @Override
    public void start() throws IOException {
        super.start();
        this.setWriter(new NoWriter());
    }

    @Override
    public String defaultFileExtension() {
        return "";
    }

    @Override
    public void renderFileViolations(Iterator<RuleViolation> violations) throws IOException {
        while (violations.hasNext()) {
            var violation = violations.next();
            System.out.println("Violation: " + violation.getFilename() + ":" + violation.getBeginLine() + ": " + violation.getDescription());
        }
    }

    @Override
    public void end() throws IOException {
        for (var err : errors) {
            System.out.println("Error: " + err.getFile() + ": " + err.getMsg());
        }
        this.getWriter().close();
    }
}
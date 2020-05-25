package nl.utwente.atelier.pmd;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Iterator;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.renderers.AbstractIncrementingRenderer;

import nl.utwente.atelier.api.AtelierAPI;
import nl.utwente.atelier.exceptions.CryptoException;
import nl.utwente.processing.LineInFile;
import nl.utwente.processing.ProcessingProject;

/** PMD violation and error renderer that submits comments to Atelier */
public class AtelierPMDRenderer extends AbstractIncrementingRenderer {

    private final String submissionID;
    private final ProcessingProject project;
    private final AtelierAPI api;

    /**
     * Create a new renderer for submitting comments to Atelier
     * @param submissionID the ID for the submission that is getting checked
     * @param files list of files that PMD is running through
     * @param api helper to create Atelier API requests
     */
    public AtelierPMDRenderer(String submissionID, ProcessingProject project, AtelierAPI api) {
        super("Atelier-" + submissionID, "Uploads comments directly to Atelier, on submission " + submissionID);
        this.submissionID = submissionID;
        this.project = project;
        this.api = api;
    }

    // Renderers are required to provide a writer, but we don't want to write
    // anything to a write, so we use the NoWriter:
    /** Writer that does absolutely nothing */
    private class NoWriter extends Writer {
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
        }
    }

    @Override
    public void start() throws IOException {
        System.out.println("Starting renderer for " + submissionID);
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

            System.out.println("Found violation for rule " + violation.getRule().getName());

            LineInFile begin, end;
            try {
                begin = project.mapJavaProjectLineNumber(violation.getBeginLine());
                end = project.mapJavaProjectLineNumber(violation.getEndLine());
                if (!begin.getFile().getId().equals(end.getFile().getId())) {
                    System.out.println("Dismissing violation: Line numbers are not in the same source file");
                    continue;
                }
            } catch (IndexOutOfBoundsException ex) {
                System.out.println("Dismissing violation: Line number is not in a source file");
                continue;
            }

            var lineStart = begin.getLine();
            var charStart = violation.getBeginColumn();
            var lineEnd = end.getLine();
            var charEnd = violation.getEndColumn();

            if (lineStart == lineEnd && charStart == charEnd) {
                var line = begin.getFile().getContent().lines().collect(Collectors.toList()).get(lineStart - 1);
                charStart = line.indexOf(line.trim());
                charEnd = line.length();
            } else {
                charStart = Math.max(0, charStart - 1);
            }

            var json = new JsonObject();
            json.addProperty("submissionID", submissionID);

            // Add snippet to the comment, Atelier uses zero-indexing
            var snippet = new JsonObject();
            var snippetStart = new JsonObject();
            snippetStart.addProperty("line", lineStart - 1);
            snippetStart.addProperty("character", charStart);
            snippet.add("start", snippetStart);
            var snippetEnd = new JsonObject();
            snippetEnd.addProperty("line", lineEnd - 1);
            snippetEnd.addProperty("character", charEnd);
            snippet.add("end", snippetEnd);
            json.add("snippet", snippet);

            // Set the default visibility to private
            json.addProperty("visibility", "private");

            // Set the text of the comment
            json.addProperty("comment", violation.getDescription());

            try {
                var res = api.postComment(begin.getFile().getId(), json);
                if (res.getStatusLine().getStatusCode() == 200) {
                    var resJson = JsonParser.parseReader(new InputStreamReader(res.getEntity().getContent()));
                    var threadID = resJson.getAsJsonObject().get("ID").getAsString();
                    System.out.println("Made comment " + threadID + " for rule " + violation.getRule().getName());
                } else {
                    System.out.println("Request to make comment failed. Got status " + res.getStatusLine().getStatusCode());
                }
            } catch (CryptoException | NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void end() throws IOException {
        for (var err : errors) {
            System.out.println("Got error: " + err.getMsg());

            var json = new JsonObject();

            json.addProperty("submissionID", submissionID);
            json.addProperty("visibility", "private");
            json.addProperty("comment", err.getMsg());

            try {
                var res = api.postProjectComment(submissionID, json);
                if (res.getStatusLine().getStatusCode() == 200) {
                    var resJson = JsonParser.parseReader(new InputStreamReader(res.getEntity().getContent()));
                    var threadID = resJson.getAsJsonObject().get("ID").getAsString();
                    System.out.println("Made comment " + threadID + " for an error");
                } else {
                    System.out.println("Request to make comment failed. Got status " + res.getStatusLine().getStatusCode());
                }
            } catch (CryptoException | NullPointerException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Ending renderer for " + submissionID);

        this.getWriter().close();
    }
}
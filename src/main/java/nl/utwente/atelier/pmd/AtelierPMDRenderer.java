package nl.utwente.atelier.pmd;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Iterator;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import nl.utwente.atelier.api.AtelierAPI;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.renderers.AbstractIncrementingRenderer;

import nl.utwente.atelier.api.Authentication;
import nl.utwente.atelier.exceptions.CryptoException;
import nl.utwente.atelier.pmd.server.Configuration;
import nl.utwente.processing.Processing;

public class AtelierPMDRenderer extends AbstractIncrementingRenderer {

    private final String fileID;
    private final String submissionID;
    private final String fileContent;
    private final AtelierAPI api;

    public AtelierPMDRenderer(String fileID, String submissionID, String fileContent, AtelierAPI api) {
        super("Atelier-" + fileID, "Uploads comments directly to Atelier, on file " + fileID);
        this.fileID = fileID;
        this.submissionID = submissionID;
        this.fileContent = fileContent;
        this.api = api;
    }

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
        System.out.println("Starting renderer for " + fileID);
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

            var lineStart = Processing.mapLineNumber(violation.getBeginLine());
            var charStart = violation.getBeginColumn();
            var lineEnd = Processing.mapLineNumber(violation.getEndLine());
            var charEnd = violation.getEndColumn();

            if (lineStart == lineEnd && charStart == charEnd) {
                var line = fileContent.lines().collect(Collectors.toList()).get(lineStart - 1);
                charStart = line.indexOf(line.trim());
                charEnd = line.length();
            } else {
                charStart = Math.max(0, charStart - 1);
            }

            var json = new JsonObject();
            json.addProperty("submissionID", submissionID);

            // Add snippet to the comment, Atelier uses zero-indexing
            var snippet = new JsonObject();
            snippet.addProperty("lineStart", lineStart - 1);
            snippet.addProperty("charStart", charStart);
            snippet.addProperty("lineEnd", lineEnd - 1);
            snippet.addProperty("charEnd", charEnd);
            json.add("snippet", snippet);

            // Set the default visibility to private
            json.addProperty("visibilityState", "private");

            // Set the text of the comment
            json.addProperty("commentBody", violation.getDescription());

            try {
                var res = api.postComment(fileID, json);
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
            json.addProperty("visibilityState", "private");
            json.addProperty("commentBody", err.getMsg());

            try {
                var res = api.postComment(fileID, json);
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

        System.out.println("Ending renderer for " + fileID);

        this.getWriter().close();
    }
}
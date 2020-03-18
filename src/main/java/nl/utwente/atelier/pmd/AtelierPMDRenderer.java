package nl.utwente.atelier.pmd;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Iterator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

            var json = new JsonObject();

            // Add snippet to the comment
            json.addProperty("lineStart", Processing.mapLineNumber(violation.getBeginLine()));
            json.addProperty("charStart", violation.getBeginColumn());
            json.addProperty("lineEnd", Processing.mapLineNumber(violation.getEndLine()));
            json.addProperty("charEnd", violation.getEndColumn());
            // TODO: add snippet body and context
            json.addProperty("snippetBody", "");
            json.addProperty("contextBefore", "");
            json.addProperty("contextAfter", "");

            // Set the default visibility to private
            json.addProperty("visibilityState", "private");

            // Set the text of the comment
            json.addProperty("commentBody", violation.getDescription());

            try {
                var commentReq = new HttpPost(config.getAtelierHost() + "/api/commentThread/file/" + fileID);
                commentReq.addHeader("Authorization", "Bearer " + auth.getCurrentToken());
                commentReq.setEntity(new StringEntity(json.toString()));

                var res = client.execute(commentReq);
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
            var json = new JsonObject();

            json.addProperty("visibilityState", "private");
            json.addProperty("commentBody", err.getMsg());

            try {
                var commentReq = new HttpPost(config.getAtelierHost() + "/api/commentThread/file/" + fileID);
                commentReq.addHeader("Authorization", "Bearer " + auth.getCurrentToken());
                commentReq.setEntity(new StringEntity(json.toString()));

                var res = client.execute(commentReq);
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
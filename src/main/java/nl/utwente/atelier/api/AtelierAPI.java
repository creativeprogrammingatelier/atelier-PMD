package nl.utwente.atelier.api;

import com.google.gson.JsonObject;
import nl.utwente.atelier.exceptions.CryptoException;
import nl.utwente.atelierpmd.server.Configuration;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;

/** Helper class to make API requests to Atelier */
public class AtelierAPI {
    private final Configuration config;
    private final Authentication auth;
    private final CloseableHttpClient client;

    public AtelierAPI(Configuration config, Authentication auth, CloseableHttpClient client) {
        this.config = config;
        this.auth = auth;
        this.client = client;
    }

    private CloseableHttpResponse makeAuthenticatedRequest(HttpRequestBase request) throws IOException, CryptoException {
        request.addHeader("Authorization", "Bearer " + auth.getCurrentToken());
        var res = client.execute(request);
        return res;
    }

    private CloseableHttpResponse makeAuthenticatedJsonRequest(String url, JsonObject json) throws IOException, CryptoException {
        var request = new HttpPost(config.getAtelierHost() + url);
        request.setEntity(new StringEntity(json.toString()));
        request.setHeader("Content-Type", "application/json");
        return makeAuthenticatedRequest(request);
    }

    /** Get the file body for a given fileID */
    public CloseableHttpResponse getFile(String fileID) throws IOException, CryptoException {
        var fileRequest = new HttpGet(config.getAtelierHost() + "/api/file/" + fileID + "/body");
        return makeAuthenticatedRequest(fileRequest);
    }

    /** Create a new comment thread on a file */
    public CloseableHttpResponse postComment(String fileID, JsonObject json) throws IOException, CryptoException {
        return makeAuthenticatedJsonRequest("/api/commentThread/file/" + fileID, json);
    }

    /** Create a new comment thread on a submission */
    public CloseableHttpResponse postProjectComment(String submissionID, JsonObject json) throws IOException, CryptoException {
        return makeAuthenticatedJsonRequest("/api/commentThread/submission/" + submissionID, json);
    }
}

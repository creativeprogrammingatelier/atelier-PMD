package nl.utwente.atelier.api;

import com.google.gson.JsonObject;
import nl.utwente.atelier.exceptions.CryptoException;
import nl.utwente.atelier.pmd.server.Configuration;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;

import java.io.IOException;

/** Helper class to make API requests to Atelier */
public class AtelierAPI {
    private final Configuration config;
    private final Authentication auth;
    private final HttpClient client;

    public AtelierAPI(Configuration config, Authentication auth, HttpClient client) {
        this.config = config;
        this.auth = auth;
        this.client = client;
    }

    private HttpResponse makeAuthenticatedRequest(HttpRequestBase request) throws IOException, CryptoException {
        request.addHeader("Authorization", "Bearer " + auth.getCurrentToken());
        var res = client.execute(request);
        request.releaseConnection();
        return res;
    }

    /** Get the file body for a given fileID */
    public HttpResponse getFile(String fileID) throws IOException, CryptoException {
        var fileRequest = new HttpGet(config.getAtelierHost() + "/api/file/" + fileID + "/body");
        return makeAuthenticatedRequest(fileRequest);
    }

    /** Create a new comment thread on a file */
    public HttpResponse postComment(String fileID, JsonObject json) throws IOException, CryptoException {
        var commentReq = new HttpPost(config.getAtelierHost() + "/api/commentThread/file/" + fileID);
        commentReq.setEntity(new StringEntity(json.toString()));
        commentReq.setHeader("Content-Type", "application/json");
        return makeAuthenticatedRequest(commentReq);
    }
}

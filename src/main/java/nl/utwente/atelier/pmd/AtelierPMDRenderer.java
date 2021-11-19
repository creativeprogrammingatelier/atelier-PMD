package nl.utwente.atelier.pmd;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.renderers.AbstractIncrementingRenderer;
import nl.utwente.atelier.api.AtelierAPI;
import nl.utwente.atelier.exceptions.CryptoException;
import nl.utwente.processing.LineInFile;
import nl.utwente.processing.ProcessingProject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

/** PMD violation and error renderer that submits comments to Atelier */
public class AtelierPMDRenderer extends AbstractIncrementingRenderer implements ErrorRenderer {

    private final String submissionID;
    private final ProcessingProject project;
    private final AtelierAPI api;

    /**
     * Create a new renderer for submitting comments to Atelier
     * @param submissionID the ID for the submission that is getting checked
     * @param project the project that PMD is running through
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
        List<JsonObject> liRuleViolations = new ArrayList<>(); // <File Name for Violation, JSON Object for API>
        Map<String, Integer> mRuleViolationStatistics  = new HashMap<>(); // <Rule Name, Violation Count>
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

            String sRuleName = mAddSpacesToString(violation.getRule().getName()).trim();
            if (mRuleViolationStatistics.containsKey(sRuleName)) {
                mRuleViolationStatistics.replace(sRuleName, mRuleViolationStatistics.get(sRuleName) + 1);
            }
            else {
                mRuleViolationStatistics.put(sRuleName, 1);
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
            json.addProperty("rule", violation.getRule().getName());
            json.addProperty("file", begin.getFile().getId());

            // Set the default visibility to private
            json.addProperty("visibility", "private");
            json.addProperty("automated", true);

            // Set the text of the comment
            json.addProperty("comment", violation.getDescription());

            liRuleViolations.add(json);
        }

        for (JsonObject oSummaryJson :
                mGetSummaryMessage(mRuleViolationStatistics)) {
            try {
                var res = api.postProjectComment(submissionID, oSummaryJson);
                if (res.getStatusLine().getStatusCode() == 200) {
                    var resJson = JsonParser.parseReader(new InputStreamReader(res.getEntity().getContent()));
                    res.close();
                    var threadID = resJson.getAsJsonObject().get("ID").getAsString();
                    System.out.println("Made ZITA Summary comment " + threadID + " for submission " + submissionID);
                } else {
                    System.out.println("Request to make comment failed. Got status " + res.getStatusLine().getStatusCode());
                }
            } catch (CryptoException | NullPointerException e) {
                e.printStackTrace();
            }
        }

        for (JsonObject json :
             liRuleViolations) {
            try {
                var res = api.postComment(json.get("file").getAsString(), json);
                if (res.getStatusLine().getStatusCode() == 200) {
                    var resJson = JsonParser.parseReader(new InputStreamReader(res.getEntity().getContent()));
                    res.close();
                    var threadID = resJson.getAsJsonObject().get("ID").getAsString();
                    System.out.println("Made comment " + threadID + " for rule " + json.get("rule").getAsString());
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
            this.renderError(err.getMsg());
        }

        System.out.println("Ending renderer for " + submissionID);

        this.getWriter().close();
    }

    @Override
    public void renderError(String errorMessage) {
        var json = new JsonObject();

        json.addProperty("submissionID", submissionID);
        json.addProperty("visibility", "private");
        json.addProperty("comment", "ZITA failed to run on this project: " + errorMessage);
        json.addProperty("automated", true);

        try {
            var res = api.postProjectComment(submissionID, json);
            if (res.getStatusLine().getStatusCode() == 200) {
                var resJson = JsonParser.parseReader(new InputStreamReader(res.getEntity().getContent()));
                res.close();
                var threadID = resJson.getAsJsonObject().get("ID").getAsString();
                System.out.println("Made comment " + threadID + " for an error");
            } else {
                System.out.println("Request to make comment failed. Got status " + res.getStatusLine().getStatusCode());
            }
        } catch (CryptoException | NullPointerException | IOException e) {
            e.printStackTrace();
        }
    }

    private List<JsonObject> mGetSummaryMessage(Map<String, Integer> mRuleViolationStatistics) {
        List<JsonObject> liResult = new ArrayList<JsonObject>();

        JsonObject oPrivateMessage = new JsonObject();
        oPrivateMessage.addProperty("submissionID", submissionID);
        oPrivateMessage.addProperty("visibility", "private");
        oPrivateMessage.addProperty("automated", true);

        JsonObject oPublicMessage = new JsonObject();
        oPublicMessage.addProperty("submissionID", submissionID);
        oPublicMessage.addProperty("visibility", "public");
        oPublicMessage.addProperty("automated", true);

        StringBuilder sbPrivateMessage = new StringBuilder();
        sbPrivateMessage.append("ZITA Summary:");

        StringBuilder sbPublicMessage = new StringBuilder();
        float iViolationScore = 0;
        boolean bDecentralizedEventHandlingRule_Flag = false;
        boolean bGodClassRule_Flag = false;
        boolean bOutOfScopeStateChangeRule_Flag = false;

        for (String sKey :
                mRuleViolationStatistics.keySet()) {
            sbPrivateMessage.append("\n  ").append(mRuleViolationStatistics.get(sKey)).append(" ").append((mRuleViolationStatistics.get(sKey) == 1) ? "violation" : "violations" ).append(" for rule \"").append(sKey).append("\".");

            switch (sKey.replace(" ", "")) {
                case "LongParameterListRule":
                case "ClassNamingConventions":
                case "FieldNamingConventions":
                case "ControlStatementBraces":
                    iViolationScore += 0.5; // Low Severity
                    break;
                case "DecentralizedEventHandlingRule":
                    iViolationScore += 2; // High Severity
                    bDecentralizedEventHandlingRule_Flag = true;
                    break;
                case "GodClassRule":
                    iViolationScore += 2; // High Severity
                    bGodClassRule_Flag = true;
                    break;
                case "OutOfScopeStateChangeRule":
                    iViolationScore += 2; // High Severity
                    bOutOfScopeStateChangeRule_Flag = true;
                    break;
                default:
                    iViolationScore += 1; // Standard Severity.
                    break;
            }
        }

        // Standard response message.
        if (iViolationScore == 0) {
            sbPublicMessage.append("There are no obvious problems in your code, but feel free to talk to a TA");
        }
        else if (iViolationScore <= 3) {
            sbPublicMessage.append("There are a few potential problems, worth discussing with a TA.");
        }
        else if (iViolationScore <= 7) {
            sbPublicMessage.append("There are many different potential problems. Please discuss your code with a TA.");
        }
        else {
            sbPublicMessage.append("There are quite a lot of potential problems with your code. Discussion with a TA is highly encouraged.");
        }

        // If serious rule violation occurs add a custom message so that the student will be notified.
        if (bDecentralizedEventHandlingRule_Flag || bGodClassRule_Flag || bOutOfScopeStateChangeRule_Flag) {
            sbPublicMessage.append("\n\nAdditionally, the following serious programming mistakes were discovered:");
            if (bDecentralizedEventHandlingRule_Flag) sbPublicMessage.append("\n  - Usage of event handling variables in draw methods.");
            if (bGodClassRule_Flag) sbPublicMessage.append("\n  - Too much responsibility given to a single class.");
            if (bOutOfScopeStateChangeRule_Flag) sbPublicMessage.append("\n  - Changing the state of variables not defined within the scope of their class or method, potentially being global.");
            sbPublicMessage.append("\n\nIt is highly encouraged to speak to a TA about the violations that have been detected");
        }

        oPrivateMessage.addProperty("comment", sbPrivateMessage.toString());
        oPublicMessage.addProperty("comment", sbPublicMessage.toString());

        liResult.add(oPrivateMessage);
        liResult.add(oPublicMessage);
        return liResult;
    }

    private String mAddSpacesToString(String sWord) {
        if (sWord.length() == 0) {
            return "";
        }
        return ((Character.isUpperCase(sWord.charAt(0)) ? " " : "") + sWord.charAt(0) + mAddSpacesToString(sWord.substring(1)));
    }
}
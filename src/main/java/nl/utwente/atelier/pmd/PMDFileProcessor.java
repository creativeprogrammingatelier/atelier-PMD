package nl.utwente.atelier.pmd;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import net.sourceforge.pmd.*;
import net.sourceforge.pmd.util.datasource.ReaderDataSource;
import nl.utwente.atelier.exceptions.PMDException;

public class PMDFileProcessor {

    // PMD Docs: https://pmd.github.io/pmd-6.22.0/pmd_userdocs_tools_java_api.html

    public void ProcessFile(InputStream file, String filename, ThreadSafeReportListener reportListener) throws PMDException {
        try {
            var config = new PMDConfiguration();
            config.setMinimumPriority(RulePriority.LOW);
            config.setRuleSets("rulesets/processing.xml");
            var ruleSetFactory = RulesetsFactoryUtils.getRulesetFactory(config);

            var ctx = new RuleContext();
            ctx.getReport().addListener(reportListener);

            var pmdFile = new ReaderDataSource(new InputStreamReader(file), filename);

            PMD.processFiles(config, ruleSetFactory, new ArrayList<>() {{
                add(pmdFile);
            }}, ctx, new ArrayList<>());
        } catch (Exception e) {
            throw new PMDException(e);
        }
    }
}
package nl.utwente.atelier.pmd;

import java.io.StringReader;
import java.util.ArrayList;

import net.sourceforge.pmd.*;
import net.sourceforge.pmd.util.datasource.ReaderDataSource;

class PMDFileProcessor {

    // PMD Docs: https://pmd.github.io/pmd-6.22.0/pmd_userdocs_tools_java_api.html

    public void ProcessFile(String file, String filename, ThreadSafeReportListener reportListener) {
        var config = new PMDConfiguration();
        config.setMinimumPriority(RulePriority.LOW);
        // TODO: get rulesets
        config.setRuleSets("rulesets/ruleset.xml");
        var ruleSetFactory = RulesetsFactoryUtils.createFactory(config);

        var ctx = new RuleContext();
        ctx.getReport().addListener(reportListener);

        var pmdFile = new ReaderDataSource(new StringReader(file), filename);

        PMD.processFiles(config, ruleSetFactory, new ArrayList<>() {{ add(pmdFile); }}, ctx, new ArrayList<>());
    }
}
package nl.utwente.atelier.pmd;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import net.sourceforge.pmd.*;
import net.sourceforge.pmd.util.ClasspathClassLoader;
import net.sourceforge.pmd.util.datasource.DataSource;
import net.sourceforge.pmd.util.datasource.ReaderDataSource;
import net.sourceforge.pmd.renderers.Renderer;
import nl.utwente.atelier.exceptions.PMDException;
import nl.utwente.processing.Processing;

public class PMDFileProcessor {

    // PMD Docs: https://pmd.github.io/pmd-6.22.0/pmd_userdocs_tools_java_api.html

    public void ProcessFile(List<PMDFile> files, Renderer renderer) throws PMDException, IOException {
        try {
            var config = new PMDConfiguration();
            config.setMinimumPriority(RulePriority.LOW);
            config.setRuleSets("rulesets/processing.xml");
            var ruleSetFactory = RulesetsFactoryUtils.getRulesetFactory(config);

            renderer.start();

            List<DataSource> datasources = files.stream().map(file -> {
                var java = Processing.toJava(file.getContent());
                return new ReaderDataSource(new StringReader(java), file.getId());
            }).collect(Collectors.toList());

            try {   
                PMD.processFiles(
                    config, 
                    ruleSetFactory, 
                    datasources, 
                    new RuleContext(), 
                    Collections.singletonList(renderer)
                );
            } finally {
                ClassLoader auxiliaryClassLoader = config.getClassLoader();
                if (auxiliaryClassLoader instanceof ClasspathClassLoader) {
                    ((ClasspathClassLoader) auxiliaryClassLoader).close();
                }
            }

            renderer.end();
            renderer.flush();
        } catch (Exception e) {
            throw new PMDException(e);
        }
    }
}
package nl.utwente.atelier.pmd;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Collections;

import net.sourceforge.pmd.*;
import net.sourceforge.pmd.util.ClasspathClassLoader;
import net.sourceforge.pmd.util.datasource.ReaderDataSource;
import net.sourceforge.pmd.renderers.Renderer;
import nl.utwente.atelier.exceptions.PMDException;
import nl.utwente.processing.Processing;

public class PMDFileProcessor {

    // PMD Docs: https://pmd.github.io/pmd-6.22.0/pmd_userdocs_tools_java_api.html

    public void ProcessFile(String fileName, InputStream file, Renderer renderer) throws PMDException, IOException {
        var processingCode = new String(file.readAllBytes());
        var javaCode = Processing.toJava(processingCode);
        var javaReader = new StringReader(javaCode);

        try {
            var config = new PMDConfiguration();
            config.setMinimumPriority(RulePriority.LOW);
            config.setRuleSets("rulesets/processing.xml");
            var ruleSetFactory = RulesetsFactoryUtils.getRulesetFactory(config);

            renderer.start();

            var datasource = new ReaderDataSource(javaReader, fileName);

            try {   
                PMD.processFiles(
                    config, 
                    ruleSetFactory, 
                    Collections.singletonList(datasource), 
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
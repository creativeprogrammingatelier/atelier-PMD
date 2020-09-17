package nl.utwente.atelier.pmd;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;

import net.sourceforge.pmd.*;
import net.sourceforge.pmd.util.ClasspathClassLoader;
import net.sourceforge.pmd.util.datasource.DataSource;
import net.sourceforge.pmd.util.datasource.ReaderDataSource;
import net.sourceforge.pmd.renderers.Renderer;

import nl.utwente.atelier.exceptions.PMDException;
import nl.utwente.processing.ProcessingProject;

/** Wrapper around PMD that allows for easy processing of projects */
public class PMDFileProcessor {

    // PMD Docs: https://pmd.github.io/pmd-6.22.0/pmd_userdocs_tools_java_api.html

    /** Run a list of files through PMD, sending the results to the provided renderer */
    public void ProcessFiles(ProcessingProject project, Renderer renderer) throws PMDException, IOException {
        try {
            var config = new PMDConfiguration();
            config.setMinimumPriority(RulePriority.LOW);
            config.setRuleSets("rulesets/atelier.xml");
            config.setIgnoreIncrementalAnalysis(true);
            var ruleSetFactory = RulesetsFactoryUtils.createFactory(config);

            renderer.start();

            List<DataSource> datasources = Collections.singletonList(
                new ReaderDataSource(new StringReader(project.getJavaProjectCode()), "Processing.pde")
            );

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
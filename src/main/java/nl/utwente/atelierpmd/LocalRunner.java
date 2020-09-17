package nl.utwente.atelierpmd;

import nl.utwente.processing.ProcessingFile;
import nl.utwente.processing.ProcessingProject;
import nl.utwente.processing.pmd.PMDException;
import nl.utwente.processing.pmd.PMDRunner;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class LocalRunner {
    // I'm sorry, but this is only for development purposes
    static String readString(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) throws IOException, PMDException {
        if (args.length < 1) {
            System.out.println("Usage: <project path>");
            return;
        }

        var path = Path.of(args[0]);
        var project = new ProcessingProject(
            Files.find(path, 6, (p, attr) -> attr.isRegularFile() && p.getFileName().toString().endsWith(".pde"))
                .map(p -> new ProcessingFile(p.getFileName().toString(), p.getFileName().toString(), readString(p)))
                .collect(Collectors.toList())
        );

        var runner = new PMDRunner();
        var renderer = new net.sourceforge.pmd.renderers.TextRenderer();
        renderer.setWriter(new PrintWriter(System.out));
        runner.Run(project, renderer);
    }
}

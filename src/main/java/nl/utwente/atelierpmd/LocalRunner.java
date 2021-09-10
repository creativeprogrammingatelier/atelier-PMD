package nl.utwente.atelierpmd;

import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.renderers.AbstractIncrementingRenderer;
import nl.utwente.processing.LineInFile;
import nl.utwente.processing.ProcessingFile;
import nl.utwente.processing.ProcessingProject;
import nl.utwente.processing.pmd.PMDException;
import nl.utwente.processing.pmd.PMDRunner;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LocalRunner {
    // I'm sorry, but this is only for development purposes
    static String readString(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    static class AtelierStyleTextRenderer extends AbstractIncrementingRenderer {
        private final ProcessingProject project;

        public AtelierStyleTextRenderer(ProcessingProject project) {
            super("AtelierStyleText", "Renders the output as text, using a style like comments on Atelier");
            this.project = project;
        }

        @Override
        public String defaultFileExtension() {
            return "";
        }

        private String padLeft(String input, int length) {
            return " ".repeat(length - input.length()) + input;
        }

        @Override
        public void renderFileViolations(Iterator<RuleViolation> violations) {
            LinkedList<String> liViolations = new LinkedList<>();
            Map<String, Integer> mViolations = new HashMap<String, Integer>();
            /* Generate Comments for Violations Found */
            while (violations.hasNext()) {
                var violation = violations.next();

                LineInFile begin, end;
                try {
                    begin = project.mapJavaProjectLineNumber(violation.getBeginLine());
                    end = project.mapJavaProjectLineNumber(violation.getEndLine());
                    if (!begin.getFile().getId().equals(end.getFile().getId())) {
                        System.out.println("! Dismissing violation of " + violation.getRule().getName() + ": Line numbers are not in the same source file\n");
                        continue;
                    }
                } catch (IndexOutOfBoundsException ex) {
                    System.out.println("! Dismissing violation of " + violation.getRule().getName() + ": Line number is not in a source file\n");
                    continue;
                }

                String sRuleName = mAddSpacesToString(violation.getRule().getName()).trim();
                if (mViolations.containsKey(sRuleName)) {
                    mViolations.replace(sRuleName, mViolations.get(sRuleName) + 1);
                }
                else {
                    mViolations.put(sRuleName, 1);
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

                StringBuilder sbViolationMessage = new StringBuilder();
                sbViolationMessage.append("> ").append(violation.getDescription()).append("\n");
                sbViolationMessage.append("  in ").append(begin.getFile().getName()).append(" line ").append(lineStart).append(":").append(charStart).append(" - ").append(lineEnd).append(":").append(charEnd).append("\n");
                var lineNumbers = IntStream.range(lineStart, lineEnd + 1).boxed().collect(Collectors.toList());
                var lineNumberLength = lineNumbers.stream().map(n -> n.toString().length()).max(Integer::compareTo).get();
                var lines = begin.getFile().getContent().lines().skip(lineStart - 1).limit(lineEnd - lineStart + 1).collect(Collectors.toList());
                for (int i = 0; i < lines.size(); i++) {
                    sbViolationMessage.append("  \033[0;37m").append(padLeft(lineNumbers.get(i).toString(), lineNumberLength)).append("\033[0m ").append(lines.get(i)).append("\n");
                }
                sbViolationMessage.append("\n");
                liViolations.addLast(sbViolationMessage.toString());
            }

            /* Generate Summary Comment  */
            StringBuilder sbSummaryMessage = new StringBuilder("ZITA Summary for:\n");
            for (String sKey :
                    mViolations.keySet()) {
                sbSummaryMessage.append("  ").append(mViolations.get(sKey)).append(" ").append((mViolations.get(sKey) == 1) ? "violation" : "violations" ).append(" for rule \"").append(sKey).append("\".\n");
            }
            sbSummaryMessage.append("\n");
            liViolations.addFirst(sbSummaryMessage.toString());

            /* Print all comments */
            for (String sMsg :
                    liViolations) {
                System.out.println(sMsg);
            }
        }

        @Override
        public void end() {
            for (var err : errors) {
                if (err.getMsg().contains("Processing.pde")) {
                    System.out.println("Error during program load, ZITA could not properly read the program files.");
                }
                else {
                    System.out.println("! Got error: " + err.getMsg() + "\n");
                }
            }
        }

        public String mAddSpacesToString(String sWord) {
            if (sWord.length() == 0) {
                return "";
            }
            return ((Character.isUpperCase(sWord.charAt(0)) ? " " : "") + sWord.charAt(0) + mAddSpacesToString(sWord.substring(1)));
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
        var renderer = new AtelierStyleTextRenderer(project);
        renderer.setWriter(new PrintWriter(System.out));
        runner.Run(project, renderer);
    }
}

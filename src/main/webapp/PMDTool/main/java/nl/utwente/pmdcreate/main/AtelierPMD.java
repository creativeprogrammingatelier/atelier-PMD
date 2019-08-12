package main.webapp.PMDTool.main.java.nl.utwente.pmdcreate.main;

import net.sourceforge.pmd.PMD;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class AtelierPMD  {

    public static final String CURRENT_DIRECTORY = ".";

    private List<File> files;
    private Map<File, String> fileContents;
    private String dirToSearch;

    public AtelierPMD(String dirToSearch) {
        files = new ArrayList<>();
        fileContents = new HashMap<>();
        this.dirToSearch = dirToSearch;
    }

    /**
     * Searches through the given directory recursively to find all .pde files
     * @param sourceDir the directory to start in
     */
    private void lookForPdeFiles(File sourceDir) {
        // depth first search
        if (sourceDir != null) {
            if (sourceDir.isDirectory()) {
                for (File file : Objects.requireNonNull(sourceDir.listFiles())) {
                    if (file.isDirectory()) {
                        lookForPdeFiles(file);
                    } else if (file.getName().endsWith(nl.utwente.pmdcreate.main.Constants.PDE_EXTENSION)) {
                        addFile(file);
                    }
                }
            }
        }
    }

    /**
     * Adds a File to a list of files.
     * @param file the File to add
     */
    private void addFile(File file) {
        files.add(file);
    }

    public List<File> getFiles() {
        return files;
    }

    public Map<File, String> getFileContents() {
        return fileContents;
    }

    /**
     * Adds a File's contents to a map of "original file -> content".
     * @param originalFile the original file of which the content originates from
     * @param content the (possible altered) content of the file
     */
    public void addFileContent(File originalFile, String content) {
        fileContents.put(originalFile, content);
    }

    /**
     * Converts all .pde files in the current directory (+ any of its subfolders) to proper Java, and saves the contents
     * to a temporary folder.
     */
    public void doWork() {
        File workingDir = new File(dirToSearch); // current dir
        for (File file : workingDir.listFiles()) {
            lookForPdeFiles(file);
        }
        makeDirIfAbsent(nl.utwente.pmdcreate.main.Constants.TEMP_FILES_DIR);
        javafyFiles();
    }

    /**
     * Creates a directory if there is none.
     * @param dir The directory to create
     */
    private void makeDirIfAbsent(String dir) {
        File file = new File(dir);
        if(!file.exists()) {
            file.mkdir();
        }
    }

    /**
     * Deletes a directory recursively.
     * @param path the directory to delete
     */
    private void deleteDirectoryStream(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    /**
     * Goes through the list of files, converts all their contents to parsable Java code, and writes these contents
     * to a similar file in ./tmp.
     */
    private void javafyFiles() {
        for (File file : getFiles()) {
            String content = readFile(file);
            content = nl.utwente.pmdcreate.main.Util.javafy(content);
//            addFileContent(file, content);
//            File file = javafiedContent.getKey();
//            String content = javafiedContent.getValue();
            try {
                // make a new file with the old directory (for easy lookup) & remove the first "./" & make it a java file
                String toDir = file.getPath()
                        .replaceAll("\\./|\\.\\\\", "")
                        .replace(nl.utwente.pmdcreate.main.Constants.PDE_EXTENSION, nl.utwente.pmdcreate.main.Constants.JAVA_EXTENSION);
                File tempFile = new File(String.format("%s/%s", nl.utwente.pmdcreate.main.Constants.TEMP_FILES_DIR, toDir));
                String completeDir = nl.utwente.pmdcreate.main.Constants.TEMP_FILES_DIR;
                String[] split = toDir.split("/|\\\\");
                for (int i = 0; i < split.length - 1; i++) {
                    String dir = split[i];
                    completeDir += "/" + dir;
                    makeDirIfAbsent(completeDir);
                }
                try (PrintWriter pw = new PrintWriter(new FileWriter(tempFile))) {
                    pw.println(content);
                }
            } catch (IOException e) {
                System.err.printf("Failed to write to file %s.%n", file.getName());
                e.printStackTrace();
            }
        }
    }

    /**
     * Reads a File's contents.
     * @param file the File to read
     * @return the File's contents
     */
    private String readFile(File file) {
        StringBuilder code = new StringBuilder();
        try {
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                code.append(scanner.nextLine()).append("\r\n");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return code.toString();
    }

    /**
     * Removes the temporary directory and all of its subdirectories.
     */
    public void removeTempFiles() {
        if (new File(nl.utwente.pmdcreate.main.Constants.TEMP_FILES_DIR).exists()) {
            try {
                deleteDirectoryStream(Paths.get(nl.utwente.pmdcreate.main.Constants.TEMP_FILES_DIR));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public  String main() {
        // TODO running out of memory (PMD issue?).
//        Scanner scanner = new Scanner(System.in);
//        boolean exists = false;
//        File dir;
//        String strDir = CURRENT_DIRECTORY;
//        System.out.println("Please input the desired relative or absolute path to search Processing files in. " +
//                "Entering nothing will result in the current directory being searched.");
//        while (!exists) {
//            strDir = scanner.nextLine();
//            if (strDir.equalsIgnoreCase("")) {
//                strDir = CURRENT_DIRECTORY;
//            }
//            dir = new File(strDir);
//            exists = dir.exists();
//            if (!exists) {
//                System.out.println("Could not find that directory, please re-enter a directory.");
//            }
//        }
//        AtelierPMD apmd = new AtelierPMD(strDir);

//        System.out.printf("Found %d files, enter any key to continue...%n", getFiles().size());

        System.out.println("PMD flag 1");
                removeTempFiles(); // so no dups arise when rerunning the program after an exception
                doWork();
        System.out.println("PMD flag 2");
        PMD.run(
                new String[]{"-d", "./tmp",
                        "-f", "xml",
                        "-R", "/home/andrew/git/pmd/PMDAtelier/src/main/webapp/PMDTool/main/resources/rulesets/processing.xml",
                        "-r", "pmd-temp-log.xml",
                        "-failOnViolation", "false"}
        );
        removeTempFiles();
        try {
           String pmdXML = (readFile("pmd-temp-log.xml", StandardCharsets.US_ASCII));
           return pmdXML;
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Finished with PMD main");
        return "No errors found";
      // remove created temporary files
//        System.out.println("Successfully removed all temporary files. Enter any key to close the program.");
    }


    public static void main(String[] args) {
        AtelierPMD apmd = new AtelierPMD("/home/andrew/Desktop/test-processing-files");
        apmd.main();
    }
    static String readFile(String path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}

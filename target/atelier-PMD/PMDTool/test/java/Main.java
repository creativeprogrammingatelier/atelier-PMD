package main.webapp.PMDTool.test.java;

import main.webapp.PMDTool.main.java.nl.utwente.pmdcreate.main.AtelierPMD;
import net.sourceforge.pmd.PMD;

import java.util.Scanner;

/**
 * Created by remco on 9-6-17.
 */
public class Main {

	public static void main(String[] args) {
		// TODO: Fix class not found + running out of memory (PMD issue?).
		AtelierPMD apmd = new AtelierPMD(AtelierPMD.CURRENT_DIRECTORY);
		apmd.removeTempFiles(); // so no dups arise when rerunning the program after an exception
		apmd.doWork();
		Scanner scanner = new Scanner(System.in);
		System.out.printf("Found %d files, enter any key to continue...%n", apmd.getFiles().size());
		scanner.next();
		PMD.main(new String[] {
				"-f", "csv",
				"-r", "log.csv",
				"-R", "rulesets/processing.xml",
				"-d", "./tmp"
		});
		apmd.removeTempFiles(); // remove created temporary files
		System.out.println("Successfully removed all temporary files. Enter any key to close the program.");
		scanner.next();
	}

}

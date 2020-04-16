package nl.utwente.processing;

// "hacks to make  processing conversion work for java parsing"
// https://github.com/swordiemen/zita/blob/master/src/main/java/nl/utwente/zita/parsing/Parser.java

/** Helper class to deal with Processing code in PMD */
public class Processing {
    private static String START_JAVA_CODE = "public class Processing {\r\n";
    private static String END_JAVA_CODE = "\r\n}";

    private static long countChars(char ch, String str) {
        return str.chars().filter(x -> x == ch).count();
    }

    /** Roughly convert Processing code to Java code */
    public static String toJava(String code) {
        code = START_JAVA_CODE + code + END_JAVA_CODE;
        code = code.replace("int(", "toInt(");
        code = code.replace("int (", "toInt (");
        code = code.replace(" = #", " = 0x");
        code = code.replace("(#", "(0x");
        code = code.replaceAll("import(.)*;", "");
        // while ((countChars('}', code) - countChars('{', code) < 2)) {
        //     code += END_JAVA_CODE;
        // }
        return code;
    }

    /** Map Java code line numbers back to Processing lines */
    public static int mapLineNumber(int line) {
        return line - (int)countChars('\n', START_JAVA_CODE);
    }
}
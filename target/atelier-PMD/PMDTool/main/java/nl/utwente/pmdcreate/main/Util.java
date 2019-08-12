package nl.utwente.pmdcreate.main;

public class Util {


    private static String START_JAVA_CODE = "public class Processing {\r\n";
    private static String END_JAVA_CODE = "\r\n}";

    public static String javafy(String code) {
        // hacks to make  processing conversion work for java parsing

        // surround with normal java class
        code = START_JAVA_CODE + code + END_JAVA_CODE;
        // replace int() with a function name
        code = code.replace("int(", "toInt(");
        code = code.replace("int (", "toInt (");
        // replace # with hex
        code = code.replace(" = #", " = 0x");
        code = code.replace("(#", "(0x");
        // replace imports with empty lines
        code = code.replaceAll("import(.)*;", "");
        // add missing curly braces
        while ((countChars('}', code) - countChars('{', code) < 2)) {
            code += END_JAVA_CODE;
        }
        return code;
    }

    public static int countChars(char c, String str) {
        int count = c;
        for (char c1 : str.toCharArray()) {
            if (c1 == c) {
                count++;
            }
        }
        return count;
    }
}

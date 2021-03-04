package it.cnr.saks.hyperion;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class Descriptor {

    private static final Map<String, String> _paramTypeMap = new HashMap<>();

    static {
        _paramTypeMap.put("byte", "B");
        _paramTypeMap.put("char", "C");
        _paramTypeMap.put("double", "D");
        _paramTypeMap.put("float", "F");
        _paramTypeMap.put("int", "I");
        _paramTypeMap.put("long", "J");
        _paramTypeMap.put("short", "S");
        _paramTypeMap.put("boolean", "Z");
        _paramTypeMap.put("void", "V");
    }

    public static String convert(String method, String retType) {
        String functionName = method.substring(0, method.indexOf('('));
        String[] params = method.replace(functionName, "").replace("(", "").replace(")", "").split(",");

        StringBuffer ret = new StringBuffer();
        ret.append(functionName);
        ret.append(":(");

        for(String arg: params) {
            String[] paramDef = arg.trim().split(" ");
            String param = paramDef[0];

            while(param.contains("[]")) {
                param = param.replaceFirst(Pattern.quote("[]"), "");
                ret.append("[");
            }

            if (_paramTypeMap.containsKey(param)) {
                ret.append(_paramTypeMap.get(param));
            } else {
                ret.append("L" + param + ";");
            }
        }

        ret.append(")");
        if (_paramTypeMap.containsKey(retType)) {
            ret.append(_paramTypeMap.get(retType));
        }
        return ret.toString();
    }

    static String[] methods = {
            "assertArrayEquals(boolean[] expecteds, boolean[] actuals)",
            "assertArrayEquals(byte[] expecteds, byte[] actuals)",
            "assertArrayEquals(char[] expecteds, char[] actuals)",
            "assertArrayEquals(double[] expecteds, double[] actuals, double delta)",
            "assertArrayEquals(float[] expecteds, float[] actuals, float delta)",
            "assertArrayEquals(int[] expecteds, int[] actuals)",
            "assertArrayEquals(long[] expecteds, long[] actuals)",
            "assertArrayEquals(Object[] expecteds, Object[] actuals)",
            "assertArrayEquals(short[] expecteds, short[] actuals)",
            "assertArrayEquals(String message, boolean[] expecteds, boolean[] actuals)",
            "assertArrayEquals(String message, byte[] expecteds, byte[] actuals)",
            "assertArrayEquals(String message, char[] expecteds, char[] actuals)",
            "assertArrayEquals(String message, double[] expecteds, double[] actuals, double delta)",
            "assertArrayEquals(String message, float[] expecteds, float[] actuals, float delta)",
            "assertArrayEquals(String message, int[] expecteds, int[] actuals)",
            "assertArrayEquals(String message, long[] expecteds, long[] actuals)",
            "assertArrayEquals(String message, Object[] expecteds, Object[] actuals)",
            "assertArrayEquals(String message, short[] expecteds, short[] actuals)",
            "assertEquals(double expected, double actual)",
            "assertEquals(double expected, double actual, double delta)",
            "assertEquals(float expected, float actual, float delta)",
            "assertEquals(long expected, long actual)",
            "assertEquals(Object[] expecteds, Object[] actuals)",
            "assertEquals(Object expected, Object actual)",
            "assertEquals(String message, double expected, double actual)",
            "assertEquals(String message, double expected, double actual, double delta)",
            "assertEquals(String message, float expected, float actual, float delta)",
            "assertEquals(String message, long expected, long actual)",
            "assertEquals(String message, Object[] expecteds, Object[] actuals)",
            "assertEquals(String message, Object expected, Object actual)",
            "assertFalse(boolean condition)",
            "assertFalse(String message, boolean condition)",
            "assertNotEquals(double unexpected, double actual, double delta)",
            "assertNotEquals(float unexpected, float actual, float delta)",
            "assertNotEquals(long unexpected, long actual)",
            "assertNotEquals(Object unexpected, Object actual)",
            "assertNotEquals(String message, double unexpected, double actual, double delta)",
            "assertNotEquals(String message, float unexpected, float actual, float delta)",
            "assertNotEquals(String message, long unexpected, long actual)",
            "assertNotEquals(String message, Object unexpected, Object actual)",
            "assertNotNull(Object object)",
            "assertNotNull(String message, Object object)",
            "assertNotSame(Object unexpected, Object actual)",
            "assertNotSame(String message, Object unexpected, Object actual)",
            "assertNull(Object object)",
            "assertNull(String message, Object object)",
            "assertSame(Object expected, Object actual)",
            "assertSame(String message, Object expected, Object actual)",
            "assertThat(String reason, T actual, Matcher<? super T> matcher)",
            "assertThat(T actual, Matcher<? super T> matcher)",
            "assertTrue(boolean condition)",
            "assertTrue(String message, boolean condition)"
    };

    public static void main(String[] args) {
        for(String met: methods) {
            System.out.println(convert(met, "void"));
        }
    }
}
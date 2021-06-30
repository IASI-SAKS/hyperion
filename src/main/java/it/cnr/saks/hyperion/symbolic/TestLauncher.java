package it.cnr.saks.hyperion.symbolic;

import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/* invoked as:
 * java -classpath "..." it.cnr.saks.hyperion.symbolic.HyperionTestLauncher testProgramClass testProgramName
 */
public class TestLauncher {
    static final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    static final String utf8 = StandardCharsets.UTF_8.name();
    static JUnitCore junit;
    static Request request;

    public static boolean runTest() {
        Result result = junit.run(request);
        return result.wasSuccessful();
    }

    public static void main(String[] args) throws Exception {
        System.out.print("[HyperionTestLauncher] Running: " + args[1] + " from class " + args[0] + "...: ");

        final Class<?> testClass = Class.forName(args[0]);

        junit = new JUnitCore();
        junit.addListener(new TextListener(new PrintStream(baos, true, utf8)));
        request = Request.method(testClass, args[1]);
        runTest();

        if(runTest()) {
            System.out.println("passed.");
        } else {
            System.out.println("failed.");
            System.out.println(baos.toString(utf8));
        }

    }
}

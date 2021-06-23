package it.cnr.saks.hyperion;

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
 * java -classpath "..."  mainClass + it.cnr.saks.hyperion.HyperionTestLauncher testProgramClass testProgramName
 */
public class HyperionTestLauncher {
    static final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    static final String utf8 = StandardCharsets.UTF_8.name();

    public static void main(String[] args) throws UnsupportedEncodingException, ClassNotFoundException {
        System.out.print("[HyperionTestLauncher] Running: " + args[1] + " from class " + args[0] + "...: ");

        Class<?> testClass = Class.forName(args[0]);

        JUnitCore junit = new JUnitCore();
        junit.addListener(new TextListener(new PrintStream(baos, true, utf8)));
        Request request = Request.method(testClass, args[1]);
        Result result = junit.run(request);

        if(result.wasSuccessful()) {
            System.out.println("passed.");
        } else {
            System.out.println("failed.");
            System.out.println(baos.toString(utf8));
        }

    }
}

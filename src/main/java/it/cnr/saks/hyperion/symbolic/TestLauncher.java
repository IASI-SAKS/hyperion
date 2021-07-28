package it.cnr.saks.hyperion.symbolic;

import org.junit.Test;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/* invoked as:
 * java -classpath "..." it.cnr.saks.hyperion.symbolic.HyperionTestLauncher testProgramClass testProgramName
 */
public class TestLauncher {

    public static void main(String[] args) throws Exception {
//        System.out.print("[HyperionTestLauncher] Running: " + args[1] + " from class " + args[0] + "...: ");
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        String utf8 = StandardCharsets.UTF_8.name();

        // The magnitude of this hack compares favorably with that of the public debt
        Class.forName("org.springframework.web.util.UriComponentsBuilder");
        Class.forName("org.mockito.internal.matchers.Any");
        Class.forName("java.util.UUID$Holder");
        Class.forName("java.util.UUID");
        Class.forName("java.lang.CharacterData00");


        final Class<?> testClass = Class.forName(args[0]);

        JUnitCore junit = new JUnitCore();
//        junit.addListener(new TextListener(new PrintStream(baos, true, utf8)));
        Request request = Request.method(testClass, args[1]);
        Result result = junit.run(request);

//        if(result.wasSuccessful()) {
//            System.out.println("passed.");
//        } else {
//            System.out.println("failed.");
//            System.out.println(baos.toString(utf8));
//        }

        System.exit(result.wasSuccessful() ? 0 : 1);
    }
}

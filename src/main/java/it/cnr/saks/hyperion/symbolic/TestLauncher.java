package it.cnr.saks.hyperion.symbolic;

import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;

/* invoked as:
 * java -classpath "..." it.cnr.saks.hyperion.symbolic.HyperionTestLauncher testProgramClass testProgramName
 */
public class TestLauncher {

    public static void main(String[] args) throws Exception {
//        System.out.print("[HyperionTestLauncher] Running: " + args[1] + " from class " + args[0] + "...: ");
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        String utf8 = StandardCharsets.UTF_8.name();

        // The magnitude of this hack compares favorably with that of the public debt
        Class.forName("org.apache.http.conn.util.InetAddressUtils");
//        Class.forName("org.mockito.internal.matchers.Any");
//        Class.forName("java.util.UUID$Holder");
//        Class.forName("java.util.UUID");
        Class.forName("java.lang.CharacterData00");
        Class.forName("org.apache.http.client.utils.URLEncodedUtils");


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

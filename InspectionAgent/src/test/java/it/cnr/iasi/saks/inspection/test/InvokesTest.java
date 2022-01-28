package it.cnr.iasi.saks.inspection.test;

import it.cnr.iasi.saks.inspection.test.sampleApplication.Application;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;

//@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
public class InvokesTest extends AbstractTest {
    private final ArrayList<String> generatedStrings = new ArrayList<>();

    public void someMethod(int i, float f, boolean b, double d, char c) {
        fakePrint("integer: " + i);
        fakePrint("float: " + f);
        fakePrint("boolean: " + b);
        fakePrint("double: " + d);
        fakePrint("char: " + c);
    }

    public void fakePrint(String msg) {
        generatedStrings.add(msg);
    }

    @Test
    public void invokeSequenceTest() {
        fakePrint( "Hello World!!!! it.test.cnr.iasi.saks.inspection.App" );
        someMethod(2, 0.1f, true, 0.2, 'c');

        String[] callers = {
                "it.cnr.iasi.saks.inspection.test.InvokesTest:invokeSequenceTest",
                "it.cnr.iasi.saks.inspection.test.InvokesTest:invokeSequenceTest",
                "it.cnr.iasi.saks.inspection.test.InvokesTest:someMethod",
                "it.cnr.iasi.saks.inspection.test.InvokesTest:someMethod",
                "it.cnr.iasi.saks.inspection.test.InvokesTest:someMethod",
                "it.cnr.iasi.saks.inspection.test.InvokesTest:someMethod",
                "it.cnr.iasi.saks.inspection.test.InvokesTest:someMethod",
                "it.cnr.iasi.saks.inspection.test.InvokesTest:invokeSequenceTest",
                "it.cnr.iasi.saks.inspection.test.AbstractTest:checkInvokes"
        };

        String[] callees = {
                "it.cnr.iasi.saks.inspection.test.InvokesTest:fakePrint",
                "it.cnr.iasi.saks.inspection.test.InvokesTest:someMethod",
                "it.cnr.iasi.saks.inspection.test.InvokesTest:fakePrint",
                "it.cnr.iasi.saks.inspection.test.InvokesTest:fakePrint",
                "it.cnr.iasi.saks.inspection.test.InvokesTest:fakePrint",
                "it.cnr.iasi.saks.inspection.test.InvokesTest:fakePrint",
                "it.cnr.iasi.saks.inspection.test.InvokesTest:fakePrint",
                "it.cnr.iasi.saks.inspection.test.AbstractTest:checkInvokes",
                "it.cnr.iasi.saks.inspection.test.AbstractTest:getMethodFromStackTrace"
        };

        Assertions.assertEquals(this.generatedStrings.size(), 6);
        this.checkInvokes(callers, callees);
    }

    @AfterEach
    public void cleanup() {
        this.generatedStrings.clear();
    }
}

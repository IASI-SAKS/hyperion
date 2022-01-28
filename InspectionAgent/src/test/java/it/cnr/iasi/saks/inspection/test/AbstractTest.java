package it.cnr.iasi.saks.inspection.test;

import it.cnr.iasi.saks.inspection.MetricsCollector;
import it.cnr.iasi.saks.inspection.SimpleInvokes;
import org.junit.jupiter.api.Assertions;

import java.util.List;

public abstract class AbstractTest {

    private static String getMethodFromStackTrace(int skip) {
        StackTraceElement stackTraceEl = Thread.currentThread().getStackTrace()[2 + skip];
        return stackTraceEl.getClassName() + ":" + stackTraceEl.getMethodName();
    }


    public void checkInvokes(String[] callers, String[] callees) {
        List<SimpleInvokes> invokes = MetricsCollector.instance().getInvokes();
        String tp = getMethodFromStackTrace(1);
        int i = 0;

        Assertions.assertEquals(callers.length, callees.length);
        Assertions.assertEquals(invokes.size(), callers.length);

        for(SimpleInvokes invoke : invokes) {
            Assertions.assertEquals(invoke.getTp(), tp);
            Assertions.assertEquals(invoke.getCaller(), callers[i]);
            Assertions.assertEquals(invoke.getCallee(), callees[i]);
            i++;
        }

        MetricsCollector.instance().wipe();
    }
}

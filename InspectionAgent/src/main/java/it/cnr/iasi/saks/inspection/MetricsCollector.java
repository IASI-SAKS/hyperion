package it.cnr.iasi.saks.inspection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class MetricsCollector {
    private static MetricsCollector instance = null;
    private static final Logger log = LoggerFactory.getLogger(MetricsCollector.class);

    private final List<SimpleInvokes> invokes = new ArrayList<>();
    private String currentTest = null;
    private final Stack<String> currentMethod = new Stack<>();
    private int seqNum = 1;
    private int frameEpoch = 1;


    private MetricsCollector() {}

    public static MetricsCollector instance() {
        if (MetricsCollector.instance == null) {
            MetricsCollector.instance = new MetricsCollector();
        }
        return instance;
    }

    public void enterMethod(String className, String methodName, Object ... parameters) {
        if(this.currentTest == null) {
            log.debug("[SKIPPED] enterMethod({}, {}): no test is running.", className, methodName);
            return;
        }
        log.debug("enterMethod({}, {})", className, methodName);

        String callee = className + ":" + methodName;

        this.invokes.add(new SimpleInvokes(this.currentTest, this.seqNum++, this.frameEpoch++, this.currentMethod.peek(), callee, parameters));
        this.currentMethod.push(callee);
    }

    public void leaveMethod(String className, String methodName) {
        log.trace("leaveMethod({}, {})", className, methodName);
        if(this.currentMethod.size() > 0)
            this.currentMethod.pop();
        assert this.frameEpoch >= 1;
    }

    public void enterTest(String className, String methodName) {
        log.trace("enterTest({}, {})", className, methodName);
        this.currentTest = className + ":" + methodName;
        this.currentMethod.push(this.currentTest);
        this.frameEpoch = 1;
        this.seqNum = 1;
    }

    public void leaveTest(String className, String methodName) {
        log.trace("leaveTest({}, {})", className, methodName);
        this.currentTest = null;
        this.currentMethod.clear();
    }

    public void printMetrics(String outFilePath) throws IOException {
        log.trace("printMetrics()");

        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(outFilePath, true), StandardCharsets.UTF_8);

        for(SimpleInvokes inv : this.getInvokes()) {
            String line = inv.getInvokeString();
            out.write(line + "\n");
            log.debug(line);
        }

        this.wipe();
        out.close();
    }

    public List<SimpleInvokes> getInvokes() {
        return this.invokes;
    }

    public void wipe() {
        this.invokes.clear();
    }
}

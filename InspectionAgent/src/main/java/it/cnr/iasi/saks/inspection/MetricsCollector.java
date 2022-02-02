package it.cnr.iasi.saks.inspection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
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

    private String outputFile = null;
    private int errorsDumping = 0;
    private static final int MAX_ERRORS = 10;


    private MetricsCollector() {}

    public static MetricsCollector instance() {
        if (MetricsCollector.instance == null) {
            MetricsCollector.instance = new MetricsCollector();
        }
        return instance;
    }

    @SuppressWarnings("unused")
    public void enterMethod(String className, String methodName, Object ... parameters) {
        if(this.currentTest == null) {
            log.debug("[SKIPPED] enterMethod({}, {}): no test is running.", className, methodName);
            return;
        }
        log.debug("enterMethod({}, {})", className, methodName);

        String callee = className + ":" + methodName;

        SimpleInvokes simpleInvokes = new SimpleInvokes(this.currentTest, this.seqNum++, this.frameEpoch++, this.currentMethod.peek(), callee, parameters);
        this.invokes.add(simpleInvokes);
        this.dumpInvokes(simpleInvokes);
        this.currentMethod.push(callee);
    }

    @SuppressWarnings("unused")
    public void leaveMethod(String className, String methodName) {
        log.trace("leaveMethod({}, {})", className, methodName);
        if(this.currentMethod.size() > 0)
            this.currentMethod.pop();
        assert this.frameEpoch >= 1;
    }

    @SuppressWarnings("unused")
    public void enterTest(String className, String methodName) {
        log.trace("enterTest({}, {})", className, methodName);
        this.currentTest = className + ":" + methodName;
        this.currentMethod.push(this.currentTest);
        this.frameEpoch = 1;
        this.seqNum = 1;
    }

    @SuppressWarnings("unused")
    public void leaveTest(String className, String methodName) {
        log.trace("leaveTest({}, {})", className, methodName);
        this.currentTest = null;
        this.currentMethod.clear();
    }

    private void dumpInvokes(SimpleInvokes simpleInvokes) {
        if(this.outputFile == null)
            return;

        try {
            FileOutputStream fos = new FileOutputStream(this.outputFile, true);
            FileChannel channel = fos.getChannel();
            FileLock lock = channel.lock();
            OutputStreamWriter out = new OutputStreamWriter(fos, StandardCharsets.UTF_8);

            String line = simpleInvokes.getInvokeString();
            out.write(line + "\n");

            lock.release();
            out.close();
        } catch (IOException e) {
            log.error("Error dumping invokes fact: {}", e.getMessage());
            if(++this.errorsDumping == MetricsCollector.MAX_ERRORS) {
                log.error("Too many errors, dumping is disabled. You will find an incomplete trace in {}", this.outputFile);
                this.outputFile = null;
            }
            e.printStackTrace();
        }

    }

    public List<SimpleInvokes> getInvokes() {
        return this.invokes;
    }

    public void wipe() {
        this.invokes.clear();
    }

    public void setOutputFile(String outFilePath) {
        this.outputFile = outFilePath;
    }
}

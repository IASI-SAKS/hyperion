package it.cnr.iasi.saks.inspection;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.instrument.Instrumentation;

public class InspectionAgent {
    private static final Logger log = LoggerFactory.getLogger(InspectionAgent.class);

    private static final String SUT_PACKAGE_PREFIX = "com.fullteaching";
    private static final String OUT_FILE_PATH = "dump.pl";

    public static void premain(String agentArgs, Instrumentation inst) {
        log.info("Registering shutdown hook");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Dumping invokes() facts...");

            try {
                int count = MetricsCollector.instance().printMetrics(OUT_FILE_PATH);
                log.info("Dumped {} facts.", count);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

        log.info("Registering transformation class");
        inst.addTransformer(new InspectionClassTransformer(SUT_PACKAGE_PREFIX, inst), false);
    }
}
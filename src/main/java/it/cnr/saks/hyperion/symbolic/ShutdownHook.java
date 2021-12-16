package it.cnr.saks.hyperion.symbolic;

import it.cnr.saks.hyperion.similarity.InformationLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShutdownHook extends Thread {
    private static final Logger log = LoggerFactory.getLogger(ShutdownHook.class);
    private static InformationLogger informationLogger;

    private ShutdownHook() {}

    private ShutdownHook(InformationLogger informationLogger) {
        ShutdownHook.informationLogger = informationLogger;
    }

    @Override
    public void run() {
        log.info("Shutdown hook activated. Saving generated facts...");
        try {
            informationLogger.emitDatalog();
        } catch (AnalyzerException e) {
            e.printStackTrace();
        }
    }

    public static void setupShutdownHook(InformationLogger informationLogger) {
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(informationLogger));
    }
}

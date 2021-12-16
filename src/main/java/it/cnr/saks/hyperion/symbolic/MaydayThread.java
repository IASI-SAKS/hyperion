package it.cnr.saks.hyperion.symbolic;

import it.cnr.saks.hyperion.AnalyzerRunnerHelper;
import it.cnr.saks.hyperion.similarity.InformationLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaydayThread extends Thread {
    private static final Logger log = LoggerFactory.getLogger(MaydayThread.class);
    private final InformationLogger informationLogger;

    public MaydayThread(InformationLogger informationLogger) {
        this.informationLogger = informationLogger;
    }

    @Override
    public void run() {
        while(true) {
            try {
                Runtime rt = Runtime.getRuntime();
                double usedMemoryPercent = (double)(rt.totalMemory() - rt.freeMemory()) / rt.totalMemory();
                log.debug("Current memory usage: {}%", usedMemoryPercent);
                if(usedMemoryPercent > 0.95)
                    break;

                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}
        }

        log.error("Out of Memory: Saving extracted facts...");
        try {
            this.informationLogger.emitDatalog();
        } catch (AnalyzerException e) {
            e.printStackTrace();
        }
        log.error("Out of Memory: Aborting execution.");
        System.exit(1);
    }
}

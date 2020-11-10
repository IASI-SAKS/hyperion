package it.cnr.saks.hyperion;

import org.jpl7.Atom;
import org.jpl7.JPL;
import org.jpl7.Query;
import org.jpl7.Term;
import org.jpl7.fli.Prolog;

import java.net.URL;
import java.util.Arrays;

public class PrologQuery {
    public static void init() throws AnalyzerException {
        if (System.getenv("SWI_HOME_DIR") != null ||
                System.getenv("SWI_EXEC_FILE") != null ||
                System.getenv("SWIPL_BOOT_FILE") != null) {
            String init_swi_config =
                    String.format("%s %s %s -g true -q --no-signals --no-packs",
                            System.getenv("SWI_EXEC_FILE") == null ? "swipl" :
                                    System.getenv("SWI_EXEC_FILE"),
                            System.getenv("SWIPL_BOOT_FILE") == null ? "" :
                                    String.format("-x %s", System.getenv("SWIPL_BOOT_FILE")),
                            System.getenv("SWI_HOME_DIR") == null ? "" :
                                    String.format("--home=%s", System.getenv("SWI_HOME_DIR")));
            System.out.println(String.format("\nSWIPL initialized with: %s", init_swi_config));

            JPL.setDefaultInitArgs(init_swi_config.split("\\s+"));
        } else
            throw new AnalyzerException("No explicit initialization done: no SWI_HOME_DIR, SWI_EXEC_FILE, or SWIPL_BOOT_FILE defined");

        JPL.init();
        System.out.println("Prolog engine actual init args: " + Arrays.toString(Prolog.get_actual_init_args()));

        // Load the prolog program
        Query q = new Query(
                "consult",
                new Term[] {new Atom(PrologQuery.class.getResource("/sim.pl").getPath())}
        );

        if(!q.hasSolution())
            throw new AnalyzerException("Unable to load prolog program.");
    }

    public static void load(String facts) throws AnalyzerException {
        Query q = new Query(
                "consult",
                new Term[] {new Atom(facts)}
        );

        if(!q.hasSolution())
            throw new AnalyzerException("Unable to load analysis facts.");
    }
}

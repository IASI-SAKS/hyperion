package it.cnr.saks.hyperion.facts;

import it.cnr.saks.hyperion.symbolic.AnalyzerException;
import org.jpl7.*;
import org.jpl7.fli.Prolog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;

public class PrologQuery {
    private static final Logger log = LoggerFactory.getLogger(PrologQuery.class);

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
            log.info("\nSWIPL initialized with: " + init_swi_config);

            JPL.setDefaultInitArgs(init_swi_config.split("\\s+"));
        } else
            throw new AnalyzerException("No explicit initialization done: no SWI_HOME_DIR, SWI_EXEC_FILE, or SWIPL_BOOT_FILE defined");

        JPL.init();
        log.info("Prolog engine actual init args: " + Arrays.toString(Prolog.get_actual_init_args()));

        new Query("set_prolog_flag(character_escapes,false)").hasSolution();

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

    public static boolean query(String function, String ... arguments) {
        Term[] terms = new Term[arguments.length];
        for(int i = 0; i < arguments.length; i++) {
            terms[i] = new Atom(arguments[i]);
        }

        Query q = new Query(function, terms);
        return q.hasSolution();
    }

    public static Map<String,Term>[] query(String function, String[] variables, String ... arguments) {
        Term[] terms = new Term[variables.length + arguments.length];

        for(int i = 0; i < variables.length; i++) {
            terms[i] = new Variable(variables[i]);
        }
        for(int i = 0; i < arguments.length; i++) {
            terms[variables.length + i] = new Atom(arguments[i]);
        }

        Query q = new Query(function, terms);
        return q.allSolutions();
    }
}

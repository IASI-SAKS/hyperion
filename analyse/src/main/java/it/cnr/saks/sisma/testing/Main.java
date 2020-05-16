package it.cnr.saks.sisma.testing;

import jbse.apps.run.RunParameters;
import jbse.apps.run.Run;
import jbse.apps.run.RunParameters.DecisionProcedureType;
import jbse.apps.run.RunParameters.StepShowMode;

import java.io.File;

public class Main {
    // Customize if needed
    public static final String Z3_PATH           = "/usr/bin/z3";

    // Information about program under test's method entry point
    private static final String METHOD_CLASS      = "it/cnr/saks/sisma/testing/example/Main";
    private static final String METHOD_NAME       = "m";
    //private static final String METHOD_DESCRIPTOR = "([Ljava/lang/String;)V";
    private static final String METHOD_DESCRIPTOR = "(I)V";

    public static void main(String[] args) {
        String path = args.length > 0 ? args[0] : "";
        final RunParameters p = configure(path);
        final Run r = new Run(p);
        r.run();
    }

    private static RunParameters configure(String path) {
        File directory;
        String[] classPath = {
                "data/jre/rt.jar",
                System.getProperty("user.dir") + "/../test/target/classes/"
        };

        if(path != "") {
            directory = new File(path);
            if (!directory.exists()) {
                if(!directory.mkdirs()) {
                    System.err.println("Unable to create output path \"" + path + "\" , aborting...");
                    System.exit(73); // EX_CANTCREAT
                }
            }
        } else {
            directory = new File("").getAbsoluteFile();
        }

        path = directory.getAbsolutePath() + File.separator;

        System.out.println("Saving results to " + path);

        RunParameters p = new RunParameters();
        p.addClasspath(classPath);
        p.setMethodSignature(METHOD_CLASS, METHOD_DESCRIPTOR, METHOD_NAME);
        p.setOutputFileName(path + "JBSE-output.txt");
        p.setSolverLogOutputfilename(path + "z3.log");
        p.setShowOnConsole(false);
        p.setDecisionProcedureType(DecisionProcedureType.Z3);
        p.setExternalDecisionProcedurePath(Z3_PATH);
        p.setStepShowMode(StepShowMode.METHOD);
        //p.setStepShowMode(StepShowMode.LEAVES);

        return p;
    }
}

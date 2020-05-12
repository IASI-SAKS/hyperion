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
    private static final String METHOD_NAME       = "main";
    private static final String METHOD_DESCRIPTOR = "([Ljava/lang/String;)V";

    public static void main(String[] args)	{
        final RunParameters p = setParameters();
        final Run r = new Run(p);
        r.run();
    }

    private static RunParameters setParameters() {
        String[] classPath = {
                "data/jre/rt.jar",
                System.getProperty("user.dir") + "/../test/target/classes/"
        };

        RunParameters p = new RunParameters();
        p.addClasspath(classPath);
        p.setMethodSignature(METHOD_CLASS, METHOD_DESCRIPTOR, METHOD_NAME);
        p.setOutputFileName("JBSE-output.txt");
        p.setShowOnConsole(false);
        p.setDecisionProcedureType(DecisionProcedureType.Z3);
        p.setExternalDecisionProcedurePath(Z3_PATH);
        p.setStepShowMode(StepShowMode.METHOD);

        return p;
    }
}

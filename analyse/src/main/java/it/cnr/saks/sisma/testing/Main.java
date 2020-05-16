package it.cnr.saks.sisma.testing;

import jbse.apps.run.RunParameters;
import jbse.apps.run.Run;
import jbse.apps.run.RunParameters.DecisionProcedureType;
import jbse.apps.run.RunParameters.StepShowMode;
import jbse.apps.run.RunParameters.StateFormatMode;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    private static String[] getClasspath() {
        String[] classPath = {
                "data/jre/rt.jar",
                System.getProperty("user.dir") + "/../test/target/classes/"
        };
        List<String> ret = new ArrayList(Arrays.asList(classPath));
        String runtimeClasspath = ManagementFactory.getRuntimeMXBean().getClassPath();
        String separator = System.getProperty("path.separator");
        String[] additionalClasspath = runtimeClasspath.split(separator);
        ret.addAll(Arrays.asList(additionalClasspath));

        String path = null;
        try {
            path = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        System.out.println("----------->" + path);

        return ret.toArray(classPath);
    }

    private static RunParameters configure(String path) {
        File directory;
        String[] classPath = getClasspath();

        System.out.println(Arrays.toString(classPath));

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
        p.addUserClasspath(classPath);
        p.setMethodSignature(METHOD_CLASS, METHOD_DESCRIPTOR, METHOD_NAME);
        p.setOutputFileName(path + "JBSE-output.txt");
        p.setSolverLogOutputfilename(path + "z3.log");
        p.setShowOnConsole(false);
        p.setDecisionProcedureType(DecisionProcedureType.Z3);
        p.setExternalDecisionProcedurePath(Z3_PATH);
        p.setStateFormatMode(StateFormatMode.TEXT);
        p.setStepShowMode(StepShowMode.METHOD);
        p.setCallback(new InspectStateCallback(path + "methods.log"));

        return p;
    }
}

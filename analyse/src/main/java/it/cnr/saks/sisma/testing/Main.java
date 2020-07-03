package it.cnr.saks.sisma.testing;

import it.cnr.saks.sisma.testing.MethodEnumerator.MethodDescriptor;
import jbse.algo.exc.NotYetImplementedException;
import jbse.bc.exc.InvalidClassFileFactoryClassException;
import jbse.common.exc.ClasspathException;
import jbse.common.exc.InvalidInputException;
import jbse.dec.exc.DecisionException;
import jbse.jvm.exc.CannotBuildEngineException;
import jbse.jvm.exc.InitializationException;
import jbse.jvm.exc.NonexistingObservedVariablesException;
import jbse.mem.exc.ContradictionException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class Main {
    private static final MethodCallSet inspector = new MethodCallSet();
    private static MethodEnumerator methodEnumerator;

    public static void main(String[] args) {
        String testPath = args.length > 0 ? args[0] : null;
        String SUTPath = args.length > 1 ? args[1] : null;

        if(testPath == null || SUTPath == null) {
            System.out.println("Need paths");
            System.exit(64); // EX_USAGE
        }

        inspector.setOutputFile(testPath + "inspection.log");
        try {
            methodEnumerator = new MethodEnumerator(testPath);
        } catch (IOException e) {
            System.exit(66); // EX_NOINPUT
        }

        for(MethodDescriptor method: methodEnumerator) {
            inspector.setCurrMethod(method.getClassName(), method.getMethodName());
            System.out.println("\tSymbolic execution starting from: " + method.getMethodName() + ", " + method.getMethodDescriptor());

            try {
                AnalyzerParameters p = configureJBSE(testPath, SUTPath, method.getClassName(), method.getMethodName(), method.getMethodDescriptor());
                Analyzer r = new Analyzer(p);
                r.run();
            } catch (CannotBuildEngineException | ContradictionException | NonexistingObservedVariablesException | InitializationException | InvalidClassFileFactoryClassException | NotYetImplementedException | DecisionException | ClasspathException | MalformedURLException | InvalidInputException e) {
                e.printStackTrace();
            }
        }

        inspector.dump();
    }

    private static AnalyzerParameters configureJBSE(String testPath, String SUTPath, String methodClass, String methodName, String methodDescriptor) throws MalformedURLException {
        URL[] urlClassPath = methodEnumerator.getClassPath();
        ArrayList<String> listClassPath = new ArrayList<>();

        listClassPath.add(SUTPath);
        for(URL u: urlClassPath) {
            listClassPath.add(u.getPath());
        }
        listClassPath.add("/home/pellegrini/Documenti/CNR/dawork/analyse/target/classes/");
        listClassPath.add("/home/pellegrini/Dropbox/Documenti/CNR/dawork/analyse/target/analyse-shaded-1.0-SNAPSHOT.jar");
        String[] classPath = new String[listClassPath.size()];
        listClassPath.toArray(classPath);

        AnalyzerParameters p = new AnalyzerParameters();
        p.addUserClasspath(classPath);
        p.setMethodSignature(methodClass.replace(".", File.separator), methodDescriptor, methodName);
        p.setDepthScope(5);
        p.methodCallSet = inspector;

        return p;
    }
}

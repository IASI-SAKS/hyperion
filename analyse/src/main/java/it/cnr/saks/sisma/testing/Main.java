package it.cnr.saks.sisma.testing;

import jbse.apps.run.Run;
import jbse.apps.run.RunParameters;
import jbse.apps.run.RunParameters.DecisionProcedureType;
import jbse.apps.run.RunParameters.StateFormatMode;
import jbse.apps.run.RunParameters.StepShowMode;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static final String Z3_PATH           = "/usr/bin/z3";
    public static final InspectStateCallback inspector = new InspectStateCallback();

    public static void main(String[] args) {
        String testPath = args.length > 0 ? args[0] : null;
        String SUTPath = args.length > 0 ? args[0] : null;

        if(testPath == null || SUTPath == null) {
            System.out.println("Need paths");
            return;
        }

        inspector.setOutputFile(testPath + "inspection.log");

        List<Class> classes = null;
        try {
            classes = enumerateClasses(testPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(66); // EX_NOINPUT
        }

        for (Class klass: classes) {
            System.out.print("Analysing class " + klass.getName() + ":");

            if(Modifier.isAbstract(klass.getModifiers())) {
                System.out.println(" Skipping, it's an abstract class.");
                continue;
            }

            inspector.setCurrClass(klass.getName());

            System.out.println(" retrieving methods...");
            Method[] m = getAccessibleMethods(klass);
            for(Method met: m) {
                boolean isTest = false;

                if(!met.getDeclaringClass().getName().equals(klass.getName()))
                    continue;

                for(Annotation ann: met.getAnnotations()) {
                    if(ann.toString().contains("@org.junit.Test")) {
                        isTest = true;
                        break;
                    }
                }

                if(!isTest)
                    continue;

                inspector.setCurrMethod(met.getName());
                System.out.println("\tSymbolic execution starting from: " + met.getName() + ", " + getMethodDescriptor(met));

                RunParameters p = null;
                try {
                    p = configureJBSE(testPath, SUTPath, klass.getName(), met.getName(), getMethodDescriptor(met));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

                Run r = new Run(p);
                try {
                    r.run();
                } catch (Exception e) { }
                finally {
                    inspector.dump();
                }
            }
        }

        inspector.dump();
    }

    static String getDescriptorForClass(final Class c)
    {
        if(c.isPrimitive())
        {
            if(c==byte.class)
                return "B";
            if(c==char.class)
                return "C";
            if(c==double.class)
                return "D";
            if(c==float.class)
                return "F";
            if(c==int.class)
                return "I";
            if(c==long.class)
                return "J";
            if(c==short.class)
                return "S";
            if(c==boolean.class)
                return "Z";
            if(c==void.class)
                return "V";
            throw new RuntimeException("Unrecognized primitive "+c);
        }
        if(c.isArray()) return c.getName().replace('.', '/');
        return ('L'+c.getName()+';').replace('.', '/');
    }

    static String getMethodDescriptor(Method m)
    {
        String s="(";
        for(final Class c:(m.getParameterTypes()))
            s+=getDescriptorForClass(c);
        s+=')';
        return s+getDescriptorForClass(m.getReturnType());
    }

    public static Method[] getAccessibleMethods(Class klass) {
        List<Method> result = new ArrayList<>();
        while (klass != null) {
            for (Method method : klass.getDeclaredMethods()) {
                int modifiers = method.getModifiers();
                if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
                    result.add(method);
                }
            }
            klass = klass.getSuperclass();
        }
        return result.toArray(new Method[result.size()]);
    }

    private static List<Class> enumerateClasses(String testPath) throws IOException {
        List<String> paths = new ArrayList<>();
        List<Class> classes = new ArrayList<>();

        Files.find(Paths.get(testPath),
                Integer.MAX_VALUE,
                (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.toString().endsWith(".class"))
                .forEach(pathVal -> paths.add(pathVal.toString()));

        for (String classFile: paths) {
            classes.add(loadClass(classFile, testPath, getClasspath(testPath)));
        }

        return classes;
    }

    private static Class loadClass(String classFile, String path, URL[] urls) {
        String classPkg = classFile.substring(0, classFile.lastIndexOf('.')).replace(path, "").replace(File.separator, ".");

        ClassLoader cl = null;
        Class<?> dynamicClass = null;

        try {
            cl = new URLClassLoader(urls);
            dynamicClass = cl.loadClass(classPkg);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return dynamicClass;
    }

    private static URL[] getClasspath(String testPath) throws MalformedURLException {
        List<URL> ret = new ArrayList<>();
        ret.add(new File(testPath).toURI().toURL());
//        ret.add(new File(SUTPath).toURI().toURL());
        ret.add(new File("data/jre/rt.jar").toURI().toURL());

        String runtimeClasspath = ManagementFactory.getRuntimeMXBean().getClassPath();
        String separator = System.getProperty("path.separator");
        String[] additionalClasspath = runtimeClasspath.split(separator);

        for(String p: additionalClasspath) {
            ret.add(new File(p).toURI().toURL());
        }

        URL[] arr = new URL[ret.size()];
        return ret.toArray(arr);
    }

    private static RunParameters configureJBSE(String testPath, String SUTPath, String methodClass, String methodName, String methodDescriptor) throws MalformedURLException {
        File directory;
        URL[] urlClassPath = getClasspath(testPath);
        ArrayList<String> listClassPath = new ArrayList<>();

        for(URL u: urlClassPath) {
            listClassPath.add(u.getPath());
        }
        listClassPath.add(SUTPath);
        listClassPath.add("/home/pellegrini/Documenti/CNR/dawork/analyse/target/classes/");
        listClassPath.add("/home/pellegrini/Dropbox/Documenti/CNR/dawork/analyse/target/analyse-shaded-1.0-SNAPSHOT.jar");
        String[] classPath = new String[listClassPath.size()];
        listClassPath.toArray(classPath);

//        System.out.println(Arrays.toString(classPath));

//        if(testPath != "") {
//            directory = new File(testPath);
//            if (!directory.exists()) {
//                if(!directory.mkdirs()) {
//                    System.err.println("Unable to create output path \"" + testPath + "\" , aborting...");
//                    System.exit(73); // EX_CANTCREAT
//                }
//            }
//        } else {
            directory = new File("").getAbsoluteFile();
//        }

        testPath = directory.getAbsolutePath() + File.separator;

        System.out.println("Saving results to " + testPath);

        RunParameters p = new RunParameters();
        p.addUserClasspath(classPath);
        p.setMethodSignature(methodClass.replace(".", File.separator), methodDescriptor, methodName);
        //p.setOutputFileName(testPath + "JBSE-output.txt");
        //p.setSolverLogOutputfilename(path + "z3.log");
        p.setShowOnConsole(false);
        p.setDecisionProcedureType(DecisionProcedureType.Z3);
        p.setExternalDecisionProcedurePath(Z3_PATH);
        p.setStateFormatMode(StateFormatMode.TEXT);
        //p.setStepShowMode(StepShowMode.METHOD);
        p.setStepShowMode(StepShowMode.METHOD);
        p.setCallback(inspector);

        return p;
    }
}

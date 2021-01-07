package it.cnr.saks.hyperion;

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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

public class MethodEnumerator implements Iterable<MethodDescriptor> {
    private final List<MethodDescriptor> methods = new ArrayList<>();
    private final Hashtable<String, ArrayList<MethodDescriptor>> beforeMethods = new Hashtable<>();
    private final URL[] classPath;
    private final List<Class> classes;
    private final List<Class> SUTClasses;

    public MethodEnumerator(String classPath, String SUTPath) throws IOException, AnalyzerException {
        this.classPath = this.initializeClasspath(classPath, SUTPath);
        this.classes = this.enumerateClasses(classPath);
        this.SUTClasses = this.enumerateClasses(SUTPath); // To load classes from the SUT

        for (Class clazz: this.classes) {
            System.out.print("Analysing class " + clazz.getName() + ":");

            if(Modifier.isAbstract(clazz.getModifiers())) {
                System.out.println(" skipping, it's an abstract class.");
                continue;
            }

            System.out.println(" retrieving valid methods...");
            Method[] methods = this.getAccessibleMethods(clazz);
            for(Method currentMethod: methods) {
                boolean isTest = false;
                boolean isBefore = false;

                if(!currentMethod.getDeclaringClass().getName().equals(clazz.getName()))
                    continue;

                for(Annotation ann: currentMethod.getAnnotations()) {
                    if(ann.toString().equals("@org.junit.Before()")) {
                        isBefore = true;
                        break;
                    }
                    if(ann.toString().contains("@org.junit.Test")) {
                        isTest = true;
                        break;
                    }
                }

                if(isBefore) {
                    if(!beforeMethods.containsKey(clazz.getName())) {
                        ArrayList<MethodDescriptor> befores = new ArrayList<>();
                        befores.add(new MethodDescriptor(currentMethod, currentMethod.getName(), this.getMethodDescriptor(currentMethod), clazz.getName()));
                        beforeMethods.put(clazz.getName(), befores);
                    } else {
                        beforeMethods.get(clazz.getName()).add(new MethodDescriptor(currentMethod, currentMethod.getName(), this.getMethodDescriptor(currentMethod), clazz.getName()));
                    }
                    continue;
                }

                if(!isTest)
                    continue;

                this.methods.add(new MethodDescriptor(currentMethod, currentMethod.getName(), this.getMethodDescriptor(currentMethod), clazz.getName()));
            }
        }
    }

    @Override
    public Iterator<MethodDescriptor> iterator() {
        return this.methods.iterator();
    }
    
    public List<MethodDescriptor> getBefores(String clazz) {
        return this.beforeMethods.get(clazz);
    }

    public Class findClass(String fqn) throws ClassNotFoundException {
        for (Class clazz: this.classes) {
            if(clazz.getName().equals(fqn))
                return clazz;
        }
        for (Class clazz: this.SUTClasses) {
            if(clazz.getName().equals(fqn))
                return clazz;
        }
        throw new ClassNotFoundException("Unable to find class " + fqn);
    }

    private String getMethodDescriptor(Method m)
    {
        StringBuilder s= new StringBuilder("(");
        for(final Class c:(m.getParameterTypes()))
            s.append(this.getDescriptorForClass(c));
        s.append(')');
        return s + this.getDescriptorForClass(m.getReturnType());
    }

    private String getDescriptorForClass(final Class c)
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

    private Method[] getAccessibleMethods(Class clazz)
    {
        List<Method> result = new ArrayList<>();
        while (clazz != null) {
            for (Method method: clazz.getDeclaredMethods()) {
                int modifiers = method.getModifiers();
                if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
                    result.add(method);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return result.toArray(new Method[result.size()]);
    }


    private List<Class> enumerateClasses(String classPath) throws IOException, AnalyzerException {
        List<String> paths = new ArrayList<>();
        List<Class> classes = new ArrayList<>();

        Files.find(Paths.get(classPath),
                Integer.MAX_VALUE,
                (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.toString().endsWith(".class"))
                .forEach(pathVal -> paths.add(pathVal.toString()));


        //ClassPathHacker.addFiles(paths); // TODO: serve?!

        for (String classFile: paths) {
            classes.add(loadClass(classFile, classPath, this.getClassPath()));
        }

        return classes;
    }

    private Class loadClass(String classFile, String path, URL[] urls) throws AnalyzerException {
        String classPkg = classFile.substring(0, classFile.lastIndexOf('.')).replace(path, "").replace(File.separator, ".");

        ClassLoader cl;
        Class<?> dynamicClass;

        try {
            cl = new URLClassLoader(urls);
            dynamicClass = cl.loadClass(classPkg);

            try {
                Class.forName(dynamicClass.getName(), true, dynamicClass.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new AssertionError(e);  // Can't happen
            }

        } catch (ClassNotFoundException e) {
            throw new AnalyzerException("Unable to find class " + e.getMessage());
        }

        return dynamicClass;
    }

    public int getMethodsCount() {
        return this.methods.size();
    }

    private URL[] initializeClasspath(String classPath, String SUTPath) throws MalformedURLException {
        List<URL> ret = new ArrayList<>();
        ret.add(new File(classPath).toURI().toURL());
        ret.add(new File(SUTPath).toURI().toURL());
        ret.add(new File("data/jre/rt.jar").toURI().toURL());

        String runtimeClasspath = ManagementFactory.getRuntimeMXBean().getClassPath();
        String separator = System.getProperty("path.separator");
        String[] additionalClasspath = runtimeClasspath.split(separator);

        for (String p: additionalClasspath) {
            ret.add(new File(p).toURI().toURL());
        }

        URL[] arr = new URL[ret.size()];
        return ret.toArray(arr);
    }

    public URL[] getClassPath() {
        return this.classPath;
    }

}

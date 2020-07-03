package it.cnr.saks.sisma.testing;

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
import java.util.Iterator;
import java.util.List;

public class MethodEnumerator implements Iterable<MethodEnumerator.MethodDescriptor> {
    private final List<MethodDescriptor> methods = new ArrayList<>();
    private final URL[] classPath;

    protected static class MethodDescriptor {
        private final Method method;
        private final String methodName;
        private final String methodDescriptor;
        private final String className;

        public MethodDescriptor(Method method, String methodName, String methodDescriptor, String className) {
            this.method = method;
            this.methodName = methodName;
            this.methodDescriptor = methodDescriptor;
            this.className = className;
        }

        public Method getMethod() {
            return method;
        }

        public String getClassName() {
            return className;
        }

        public String getMethodDescriptor() {
            return methodDescriptor;
        }

        public String getMethodName() {
            return methodName;
        }
    }

    public MethodEnumerator(String classPath) throws IOException, AnalyzerException {
        this.classPath = this.initializeClasspath(classPath);

        List<Class> classes = this.enumerateClasses(classPath);

        for (Class clazz: classes) {
            System.out.print("Analysing class " + clazz.getName() + ":");

            if(Modifier.isAbstract(clazz.getModifiers())) {
                System.out.println(" skipping, it's an abstract class.");
                continue;
            }

            System.out.println(" retrieving valid methods...");
            Method[] m = this.getAccessibleMethods(clazz);
            for(Method met: m) {
                boolean isTest = false;

                if(!met.getDeclaringClass().getName().equals(clazz.getName()))
                    continue;

                for(Annotation ann: met.getAnnotations()) {
                    if(ann.toString().contains("@org.junit.Test")) {
                        isTest = true;
                        break;
                    }
                }

                if(!isTest)
                    continue;

                methods.add(new MethodDescriptor(met, met.getName(), this.getMethodDescriptor(met), clazz.getName()));
            }
        }
    }

    @Override
    public Iterator<MethodDescriptor> iterator() {
        return this.methods.iterator();
    }

    private String getMethodDescriptor(Method m)
    {
        String s="(";
        for(final Class c:(m.getParameterTypes()))
            s += this.getDescriptorForClass(c);
        s+=')';
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

    private Method[] getAccessibleMethods(Class klass)
    {
        List<Method> result = new ArrayList<>();
        while (klass != null) {
            for (Method method: klass.getDeclaredMethods()) {
                int modifiers = method.getModifiers();
                if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
                    result.add(method);
                }
            }
            klass = klass.getSuperclass();
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

        for (String classFile: paths) {
            classes.add(loadClass(classFile, classPath, this.getClassPath()));
        }

        return classes;
    }

    private Class loadClass(String classFile, String path, URL[] urls) throws AnalyzerException {
        String classPkg = classFile.substring(0, classFile.lastIndexOf('.')).replace(path, "").replace(File.separator, ".");

        ClassLoader cl;
        Class<?> dynamicClass = null;

        try {
            cl = new URLClassLoader(urls);
            dynamicClass = cl.loadClass(classPkg);
        } catch (ClassNotFoundException e) {
            throw new AnalyzerException(e.getMessage());
        }

        return dynamicClass;
    }

    public int getMethodsCount() {
        return this.methods.size();
    }

    private URL[] initializeClasspath(String classPath) throws MalformedURLException {
        List<URL> ret = new ArrayList<>();
        ret.add(new File(classPath).toURI().toURL());
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

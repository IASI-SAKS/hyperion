package it.cnr.iasi.saks.inspection;

import javassist.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class InspectionClassTransformer implements ClassFileTransformer {
    private static final Logger log = LoggerFactory.getLogger(InspectionClassTransformer.class);

    private final String sutPackagePrefix;

    public InspectionClassTransformer(String sutPackagePrefix) {
        this.sutPackagePrefix = sutPackagePrefix;
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if(className == null)
            return null;

        String clazzName = className.replace("/", ".");

        // Skip all agent classes, but instrument the project's test package
        if (clazzName.startsWith("it.cnr.iasi.saks") && !clazzName.startsWith("it.cnr.iasi.saks.inspection.test")) {
            log.trace("Skipping class {}: belongs to the agent", clazzName);
            return null;
        }

        // Skip class if it doesn't belong to our SUT program or relevant code
        if (!clazzName.startsWith(this.sutPackagePrefix)
                && !clazzName.startsWith("it.cnr.iasi.saks.inspection.test")
                && !clazzName.startsWith("org.springframework.test.web.servlet")) {
            log.trace("Skipping class {}: not part of the SUT", clazzName);
            return null;
        }

        // Skip known proxy classes
        if(clazzName.contains("CGLIB$$") || clazzName.contains("$Proxy") || clazzName.contains("$HibernateProxy$"))
            return null;

        // Build a dedicated class pool for this instrumentation and import required packaged
        ClassPool pool = new ClassPool();
        pool.appendClassPath(new LoaderClassPath(loader));
        pool.importPackage("it.cnr.iasi.saks.inspection");
        pool.importPackage(this.sutPackagePrefix);

        // Build the class representation
        CtClass cclass;
        try {
            cclass = pool.makeClass(new ByteArrayInputStream(classfileBuffer));
        } catch (IOException e) {
            log.error("Unable to build CtClass for class: {}", clazzName);
            e.printStackTrace();
            return null;
        }
        assert cclass != null;

        // Skip interfaces
        if(cclass.isInterface())
            return null;

        for (CtMethod method : cclass.getDeclaredMethods()) {

            if(Modifier.isNative(method.getModifiers()) || Modifier.isAbstract(method.getModifiers()))
                continue;

            // Get possible method annotations
            Object annotation4 = null;
            Object annotation5 = null;
            try {
                annotation4 = method.getAnnotation(org.junit.Test.class);
                annotation5 = method.getAnnotation(org.junit.jupiter.api.Test.class);
            } catch (ClassNotFoundException ignored) {}

            // Insert the instrumentation code at the start/end of the method.
            try {
                if(annotation4 != null || annotation5 != null) {
                    method.insertBefore("MetricsCollector.instance().enterTest(\"" + clazzName + "\",\"" + method.getName() + "\");");
                    method.insertAfter("MetricsCollector.instance().leaveTest(\"" + clazzName + "\",\"" + method.getName() + "\");");
                } else {
                    method.insertBefore("MetricsCollector.instance().enterMethod(\"" + clazzName + "\",\"" + method.getName() + "\", $args);");
                    method.insertAfter("MetricsCollector.instance().leaveMethod(\"" + clazzName + "\",\"" + method.getName() + "\");");
                }
            } catch (CannotCompileException e) {
                log.error("Unable to instrument class {}. The class has not been instrumented.", clazzName);
                e.printStackTrace();
                return null;
            }
        }

        try {
            classfileBuffer = cclass.toBytecode();
            cclass.detach();
            return classfileBuffer;
        } catch (IOException | CannotCompileException e) {
            log.error("Unable to generate instrumented bytecode for class {}. The class has not been instrumented.", clazzName);
            e.printStackTrace();
        }

        cclass.detach();
        return null;
    }
}
package it.cnr.iasi.saks.inspection;

import javassist.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class InspectionClassTransformer implements ClassFileTransformer {
    private static final Logger log = LoggerFactory.getLogger(InspectionClassTransformer.class);

    private final MetricsCollector collector = MetricsCollector.instance();
    private final ClassPool pool;
    private final String sutPackagePrefix;

    public InspectionClassTransformer(String sutPackagePrefix) {
        this.pool = ClassPool.getDefault();
        this.sutPackagePrefix = sutPackagePrefix;
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if(className == null)
            return null;

        String clazzName = className.replace("/", ".");

//        if(clazzName.contains("_$$_jvst"))
//            clazzName = StringUtils.substringBefore(clazzName, "_$$_jvst");

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

        // Skip classes injected by cglib
        if(clazzName.contains("CGLIB"))
            return null;

        // Records a package name so that the Javassist compiler may resolve a class name.
        // This enables the compiler to resolve the class "MetricsCollector" below
        pool.importPackage("it.cnr.iasi.saks.inspection");
        pool.importPackage(this.sutPackagePrefix);

        // Retrieve the class representation i.e. CtClass object
        CtClass cclass;
        try {
            cclass = pool.get(clazzName);
        } catch (NotFoundException e) {
            log.error("Class not found: {}", clazzName);
            e.printStackTrace();
            return null;
        }
        assert cclass != null;

        // Skip interfaces
        if(cclass.isInterface())
            return null;

        for (CtMethod method : cclass.getDeclaredMethods()) {

            if(Modifier.isNative(method.getModifiers()))
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

                classfileBuffer = cclass.toBytecode();
                cclass.detach();
                return classfileBuffer;
            } catch (CannotCompileException | IOException e) {
                log.error("Unable to instrument class {}. The class has not been instrumented.", clazzName);
                e.printStackTrace();
            }

        }

        return null;
    }
}
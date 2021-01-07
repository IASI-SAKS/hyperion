package it.cnr.saks.hyperion;

import javassist.*;
import jbse.bc.Signature;

import java.io.IOException;
import java.util.List;

public class TestWrapper {
    ClassPool cp;

    public TestWrapper(String[] runtimeClasspath) throws NotFoundException {
        this.cp = ClassPool.getDefault();
        for(String path: runtimeClasspath) {
            cp.insertClassPath(path);
        }
    }

    public static void run() { // rinomina
        // Here code is injected at runtime
    }

    public void generateWrapper(Signature testProgramSignature, List<MethodDescriptor> befores) {
        String testProgramClassName = testProgramSignature.getClassName();
        String testProgramMethodName = testProgramSignature.getName();

        try {
            CtClass selfKlass = this.cp.get(this.getClass().getName());
            CtClass testKlass = this.cp.get(testProgramClassName);

            CtMethod methodToModify = selfKlass.getDeclaredMethod("run");
            String[] bits = testKlass.getName().split("/");
            String classNameForDeclaration = bits[bits.length-1];

            this.cp.clearImportedPackages();
            this.cp.importPackage(testProgramSignature.getClassName().replace("/","."));

            StringBuffer body = new StringBuffer();
            body.append("{\n");
            body.append(classNameForDeclaration + " kl = new " + classNameForDeclaration + "();\n");

            for(MethodDescriptor before: befores) {
                body.append("kl."+before.getMethodName()+"();\n");
            }

            body.append("kl." + testProgramSignature.getName() + "();\n");
            body.append("}");

            System.out.println("wrapper method body:");
            System.out.println(body.toString());

            methodToModify.setBody(body.toString());
            selfKlass.writeFile("target/classes/"); // TODO: what if we're in a JAR?

        } catch (CannotCompileException | NotFoundException | IOException ex) {
            ex.printStackTrace();
        }
    }
}

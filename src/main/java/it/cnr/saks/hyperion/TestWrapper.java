package it.cnr.saks.hyperion;

import javassist.*;
import jbse.bc.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class TestWrapper {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    final ClassPool cp;
    final CtClass selfKlass;

    public TestWrapper(URL[] runtimeClasspath) throws NotFoundException {
        this.cp = ClassPool.getDefault();
        for(URL path: runtimeClasspath) {
            cp.insertClassPath(path.getPath());
        }
        this.selfKlass = this.cp.get(this.getClass().getName());
        selfKlass.stopPruning(true);

    }

    public static void wrapperEntryPoint() {
        // Here code is injected at runtime
    }

    public void generateWrapper(Signature testProgramSignature, List<MethodDescriptor> befores) {

        try {
            CtClass testKlass = this.cp.get(testProgramSignature.getClassName());

            CtMethod methodToModify = this.selfKlass.getDeclaredMethod("wrapperEntryPoint");
            String[] bits = testKlass.getName().split("/");
            String classNameForDeclaration = bits[bits.length-1];

            this.cp.clearImportedPackages();
            this.cp.importPackage(testProgramSignature.getClassName().replace("/","."));

            StringBuilder body = new StringBuilder();
            body.append("{\n");
            body.append(classNameForDeclaration + " obj = new " + classNameForDeclaration + "();\n");

            for(MethodDescriptor before: befores) {
                body.append("obj."+before.getMethodName()+"();\n");
            }

            body.append("obj." + testProgramSignature.getName() + "();\n");
            body.append("}");

            log.info("wrapper method body:");
            log.info(body.toString());

            if (this.selfKlass.isFrozen())
                this.selfKlass.defrost();

            methodToModify.setBody(body.toString());
            this.selfKlass.writeFile("target/classes/"); // TODO: what if we're in a JAR?
        } catch (CannotCompileException | NotFoundException | IOException ex) {
            ex.printStackTrace();
        }
    }
}

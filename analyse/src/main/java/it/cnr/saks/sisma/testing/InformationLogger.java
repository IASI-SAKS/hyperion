package it.cnr.saks.sisma.testing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jbse.bc.ClassFile;
import jbse.bc.Signature;
import jbse.mem.*;
import jbse.mem.exc.FrozenStateException;
import jbse.mem.exc.InvalidSlotException;
import jbse.mem.exc.ThreadStackEmptyException;
import jbse.val.Reference;
import jbse.val.ReferenceSymbolicMemberField;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;

import static jbse.algo.Util.valueString;

public class InformationLogger {
    private final MethodEnumerator methodEnumerator;
    private PrintStream out = null;
    // Class -> Method -> Information Data
    private final HashMap<String, HashMap<String, TestInformation>> methodCalls = new HashMap<>();
    private String currClass;
    private String currMethod;

    public InformationLogger(MethodEnumerator methodEnumerator) {
        this.methodEnumerator = methodEnumerator;
    }

    public void inspectState(State s) {
        if(s.isStuck())
            return;
        try {
            Signature method = s.getCurrentMethodSignature();
            String name = method.getName();
            ClassFile classFile = s.getCurrentClass();

            if((name.equals("get") || name.equals("post") || name.equals("put") || name.equals("delete") )
                    && classFile.getClassName().equals("org/springframework/test/web/servlet/request/MockMvcRequestBuilders")) {
                HeapObjekt parameter = s.getObject((Reference) s.getCurrentFrame().getLocalVariableValue(0));

                if(parameter != null) {
                    String value = null;

                    if (parameter.isSymbolic()) {
                        String className = ((ReferenceSymbolicMemberField)parameter.getOrigin()).getFieldClass().replace('/', '.');
                        String fieldName = ((ReferenceSymbolicMemberField)parameter.getOrigin()).getFieldName();
                        Class<?> clazz = this.methodEnumerator.findClass(className);
                        Field f = clazz.getDeclaredField(fieldName);
                        boolean accessible = f.isAccessible();
                        if(!accessible)
                            f.setAccessible(true);
                        value = (String)f.get(null);
                        if(!accessible)
                            f.setAccessible(false);
                    } else {
                        value = valueString(s, (Reference) s.getCurrentFrame().getLocalVariableValue(0)); // OK!
                    }
                    this.methodCalls.get(this.currClass).get(this.currMethod).addEndPoint(name, value, null);
                }
            }
            this.methodCalls.get(this.currClass).get(this.currMethod).addMethodCall(name, method.getDescriptor(), classFile.getClassName());

        } catch (FrozenStateException | InvalidSlotException | ClassNotFoundException | NoSuchFieldException | IllegalAccessException | ThreadStackEmptyException e) {
            System.err.print(e.getMessage());
        }
    }

    public void setOutputFile(String f) {
        final File file = new File(f);
        try {
            this.out = new PrintStream(file);
        } catch (FileNotFoundException e) { }
    }

    public void setCurrMethod(String currClass, String currMethod) {
        this.currClass = currClass;
        this.currMethod = currMethod;
        if(!methodCalls.containsKey(currClass)) {
            methodCalls.put(currClass, new HashMap<>());
        }
        methodCalls.get(this.currClass).put(currMethod, new TestInformation());
    }

    public void dump() throws JsonProcessingException {
        if(this.out == null)
            return;

        methodCalls.forEach((key,value) -> {
            if(value.size() == 0)
                return;
            this.out.println("Class: " + key);
            value.forEach((key2, value2) -> {
                this.out.println("\t Method: " + key2);
                HashSet<TestInformation.MethodCall> methods = value2.getMethodCalls();
                methods.forEach( (value3) -> {
                   this.out.println("\t\t" + value3.getClassName() + " " + value3.getMethodName() + " " + value3.getMethodDescriptor() + " " + value3.getHits());
                });
            });
        });
    }
}

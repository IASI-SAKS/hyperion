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
import java.util.HashMap;

import static jbse.algo.Util.valueString;

public class InformationLogger {
    private final MethodEnumerator methodEnumerator;
    private PrintStream jsonOut = null;
    // Class -> Method -> Information Data
    private final HashMap<String, HashMap<String, TestInformation>> loggedInformation = new HashMap<>();
    private String currClass;
    private String currMethod;

    public InformationLogger(MethodEnumerator methodEnumerator) {
        this.methodEnumerator = methodEnumerator;
    }

    public void onMethodCall(State s) {
        Signature method = null;
        ClassFile classFile = null;

        if(s.isStuck())
            return;

        try {
            method = s.getCurrentMethodSignature();
            classFile = s.getCurrentClass();
        } catch (ThreadStackEmptyException e) {
            e.printStackTrace();
            return;
        }

        String name = method.getName();
        if(name.equals("<init>"))
            return;

        if((name.equals("get") || name.equals("post") || name.equals("put") || name.equals("delete") )
                && classFile.getClassName().equals("org/springframework/test/web/servlet/request/MockMvcRequestBuilders")) {
            this.inspectHttpRequest(s, name);
        }

        this.loggedInformation.get(this.currClass).get(this.currMethod).addMethodCall(name, method.getDescriptor(), classFile.getClassName());
    }

    public void setJsonOutputFile(String f) {
        final File file = new File(f);
        try {
            this.jsonOut = new PrintStream(file);
        } catch (FileNotFoundException e) {
            this.jsonOut = null;
        }
    }

    public void setCurrMethod(String currClass, String currMethod) {
        this.currClass = currClass;
        this.currMethod = currMethod;
        if(!loggedInformation.containsKey(currClass)) {
            loggedInformation.put(currClass, new HashMap<>());
        }
        loggedInformation.get(this.currClass).put(currMethod, new TestInformation());
    }

    public void emitJson() throws JsonProcessingException {
        if(this.jsonOut == null)
            return;

        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        String jsonString = mapper.writeValueAsString(this.loggedInformation);
        this.jsonOut.println(jsonString);
    }

    private void inspectHttpRequest(State s, String name) {
        try {
            HeapObjekt parameter = s.getObject((Reference) s.getCurrentFrame().getLocalVariableValue(0));

            if(parameter != null) {
                String value;

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
                this.loggedInformation.get(this.currClass).get(this.currMethod).addEndPoint(name, value, null);
            }
        } catch (FrozenStateException | ThreadStackEmptyException | InvalidSlotException | ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}

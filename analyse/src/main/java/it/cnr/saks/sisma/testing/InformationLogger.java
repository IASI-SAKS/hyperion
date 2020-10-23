package it.cnr.saks.sisma.testing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jbse.bc.ClassFile;
import jbse.bc.Signature;
import jbse.mem.*;
import jbse.mem.exc.FrozenStateException;
import jbse.mem.exc.InvalidNumberOfOperandsException;
import jbse.mem.exc.InvalidSlotException;
import jbse.mem.exc.ThreadStackEmptyException;
import jbse.val.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.*;

import static jbse.algo.Util.valueString;
import static jbse.common.Type.parametersNumber;
import static jbse.common.Type.splitParametersDescriptors;

public class InformationLogger {
    private final MethodEnumerator methodEnumerator;
    private PrintStream jsonOut = null;
    private PrintStream datalogOut = null;
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

        int stackFrames = s.getStackSize();
        if(stackFrames < 2)
            return;

        try {
            method = s.getCurrentMethodSignature();
            classFile = s.getCurrentClass();
        } catch (ThreadStackEmptyException e) {
            e.printStackTrace();
            return;
        }

        String name = method.getName();

        Signature caller = null;
        int callerPC = -1;
        try {
             caller = s.getStack().get(stackFrames - 2).getMethodSignature();
             callerPC = s.getStack().get(stackFrames - 2).getProgramCounter();
        } catch (FrozenStateException e) {
            e.printStackTrace();
        }

        if(name.equals("<init>") || caller.getName().equals("<init>"))
            return;

        String branchId = s.getBranchIdentifier().substring(1);
        String pathId = "[" + branchId.replaceAll("\\.", ", ") + "], " + s.getSequenceNumber();

        if((name.equals("get") || name.equals("post") || name.equals("put") || name.equals("delete") )
                && classFile.getClassName().equals("org/springframework/test/web/servlet/request/MockMvcRequestBuilders")) {
            this.inspectHttpRequest(s, name, pathId);
        }

        String programPoint = caller.getClassName() + ":" + caller.getDescriptor() + ":" + callerPC;

        this.loggedInformation.get(this.currClass).get(this.currMethod).addMethodCall(name, method.getDescriptor(), classFile.getClassName(), pathId, programPoint);
    }

    public void setJsonOutputFile(String f) {
        final File file = new File(f);
        try {
            this.jsonOut = new PrintStream(file);
        } catch (FileNotFoundException e) {
            this.jsonOut = null;
        }
    }

    public void setDatalogOutputFile(String f) {
        final File file = new File(f);
        try {
            this.datalogOut = new PrintStream(file);
        } catch (FileNotFoundException e) {
            this.datalogOut = null;
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

    public void emitDatalog() {
        if(this.datalogOut == null)
            return;

        this.loggedInformation.forEach((klass,methodsInKlass) -> { // for each class
            if(methodsInKlass.size() == 0)
                return;
            methodsInKlass.forEach((method, methodLoggedInformation) -> { // for each analyzed method

                // Process method calls
                ArrayList<TestInformation.MethodCall> methodCalls = methodLoggedInformation.getMethodCalls();
                for (ListIterator<TestInformation.MethodCall> iterator = methodCalls.listIterator(); iterator.hasNext(); ) {
                    TestInformation.MethodCall methodCall = iterator.next();

                    this.datalogOut.println("invokes("
                            + klass + ":" + method + ", "
                            + methodCall.getPathId() + ", " + methodCall.getProgramPoint() + ", " + "[PATH CONDITION]" + ", "
                            + methodCall.getClassName() + ":" + methodCall.getMethodName() + ":" + methodCall.getMethodDescriptor() + ", "
                            + "[PARAMETRI]" + ")");

//                    this.datalogOut.println("uses(" + klass + ":" + method + ", " + methodCall.getClassName() + ")");

//                    for (ListIterator<TestInformation.MethodCall> i2 = methodCalls.listIterator(iterator.nextIndex()); i2.hasNext(); ) {
//                        TestInformation.MethodCall subsequentMethodCall = i2.next();
//                        this.datalogOut.println("invokesBefore(" + klass + ":" + method + ", "
//                                + methodCall.getClassName() + ":" + methodCall.getMethodName() + ":" + methodCall.getMethodDescriptor() + ", "
//                                + subsequentMethodCall.getClassName() + ":" + subsequentMethodCall.getMethodName() + ":" + subsequentMethodCall.getMethodDescriptor() + ")");
//                    }
                }

                // Process endpoints
                ArrayList<TestInformation.EndPoint> endpoints = methodLoggedInformation.getEndPoints();
                for (ListIterator<TestInformation.EndPoint> iterator = endpoints.listIterator(); iterator.hasNext(); ) {
                    TestInformation.EndPoint endpoint = iterator.next();

//                    this.datalogOut.println("contacts(" + klass + ":" + method + ", "
//                            + endpoint.getType() + ", " + endpoint.getEndPoint() + ", " + endpoint.getPathId() + ")");

//                    for (ListIterator<TestInformation.EndPoint> i2 = endpoints.listIterator(iterator.nextIndex()); i2.hasNext(); ) {
//                        TestInformation.EndPoint subsequentEndpoint = i2.next();
//                        this.datalogOut.println("contactsBefore(" + klass + ":" + method + ", "
//                                + endpoint.getType() + ", " + endpoint.getEndPoint() + ", " + endpoint.getPathId() + ", "
//                                + subsequentEndpoint.getType() + ", " + subsequentEndpoint.getEndPoint() + ", " + subsequentEndpoint.getPathId() + ")");
//                    }
                }
            });
        });
    }

    private void inspectHttpRequest(State s, String name, String pathId) {
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
                this.loggedInformation.get(this.currClass).get(this.currMethod).addEndPoint(name, value, null, pathId);
            }
        } catch (FrozenStateException | ThreadStackEmptyException | InvalidSlotException | ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void inspectMethodCall(State s, String name, Signature method, ClassFile classFile) {

//        TestInformation.MethodCall md = this.loggedInformation.get(this.currClass).get(this.currMethod).addMethodCall(name, method.getDescriptor(), classFile.getClassName());

        TestInformation.MethodCall md = null;
        Value[] operands = null;
        int numOperands = splitParametersDescriptors(method.getDescriptor()).length;

        try {
            operands = s.getCurrentFrame().operands(numOperands);
        } catch (InvalidNumberOfOperandsException | ThreadStackEmptyException | FrozenStateException e) {
            return;
        }

        TestInformation.ParameterSet pSet = new TestInformation.ParameterSet();

        for (Value op : operands) {
            try {
                if (op instanceof Simplex) {
                    pSet.addParameter(((Simplex) op).getActualValue());
                } else {
                    if(op.isSymbolic()) {
                        pSet.addParameter("SYMBOLIC");
                    } else {
                        HeapObjekt operand = s.getObject((Reference) op);

                        if (operand == null) {
                            pSet.addParameter(null); // ???
                            continue;
                        }

                        String className = operand.getType().getClassName();
                        if (className.equals("java/lang/String")) {
                            String string = valueString(s, (Instance) operand);
                            pSet.addParameter(string);
                        } else {
                            pSet.addParameter(className); // ???
                        }
                    }
                }
            } catch (FrozenStateException e) {
                return;
            }
        }

        md.addInvocation(pSet);
    }
}

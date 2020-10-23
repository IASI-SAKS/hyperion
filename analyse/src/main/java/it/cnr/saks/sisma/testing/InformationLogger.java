package it.cnr.saks.sisma.testing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jbse.bc.ClassFile;
import jbse.bc.Signature;
import jbse.common.Type;
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
import static jbse.apps.Util.LINE_SEP;
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
        String programPoint = caller.getClassName() + ":" + caller.getDescriptor() + "@" + callerPC;

        if((name.equals("get") || name.equals("post") || name.equals("put") || name.equals("delete") )
                && classFile.getClassName().equals("org/springframework/test/web/servlet/request/MockMvcRequestBuilders")) {
            this.inspectHttpRequest(s, name, pathId);
        }

        this.inspectMethodCall(s, name, method, classFile, pathId, programPoint);
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
                            + methodCall.getParameterSet().getParameters() + ")");

                    this.datalogOut.println("\n" + methodCall.getPathCondition() + "\n");

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

    private void inspectMethodCall(State s, String name, Signature method, ClassFile classFile, String pathId, String programPoint) {

        StringBuilder sb = new StringBuilder();
        try {
            formatPathCondition(s, sb);
        } catch (FrozenStateException e) {
            e.printStackTrace();
        }

        TestInformation.MethodCall md = this.loggedInformation.get(this.currClass).get(this.currMethod).addMethodCall(name, method.getDescriptor(), classFile.getClassName(), pathId, programPoint, sb.toString());
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
//                    Simplex v = ((Simplex) op).getActualValue();
                    pSet.addParameter(op.toString());
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

        md.setParameterSet(pSet);
    }

    private static void formatPathCondition(State s, StringBuilder sb)
            throws FrozenStateException {
        final StringBuilder expression = new StringBuilder();
        final StringBuilder where = new StringBuilder();
        boolean doneFirstExpression = false;
        boolean doneFirstWhere = false;
        HashSet<String> doneSymbols = new HashSet<String>();
        for (Clause c : s.getPathCondition()) {
            if (c instanceof ClauseAssume) {
                expression.append(doneFirstExpression ? " &&" : "");
                doneFirstExpression = true;
                final Primitive cond = ((ClauseAssume) c).getCondition();
                formatValue(s, expression, cond);
                final StringBuilder expressionWhereCondition = new StringBuilder();
                final boolean some = formatValueForPathCondition(cond, expressionWhereCondition, doneSymbols);
                if (some) {
                    where.append(doneFirstWhere ? " &&" : ""); where.append(expressionWhereCondition);
                    doneFirstWhere = true;
                } //else does nothing
            } else if (c instanceof ClauseAssumeReferenceSymbolic) {
                expression.append(doneFirstExpression ? " &&" : "");
                doneFirstExpression = true;
                final ReferenceSymbolic ref = ((ClauseAssumeReferenceSymbolic) c).getReference();
                expression.append(ref.toString()); expression.append(" == ");
                if (s.isNull(ref)) {
                    expression.append("null");
                } else {
                    final ReferenceSymbolic tgtOrigin = s.getObject(ref).getOrigin();
                    expression.append("Object["); expression.append(s.getResolution(ref)); expression.append("] ("); expression.append(ref.equals(tgtOrigin) ? "fresh" : ("aliases " + tgtOrigin)); expression.append(")");
                }
                final StringBuilder referenceFormatted = new StringBuilder();
                final boolean someText = formatValueForPathCondition(ref, referenceFormatted, doneSymbols);
                if (someText) {
                    if (doneFirstWhere) {
                        where.append(" &&");
                    }
                    where.append(referenceFormatted);
                    doneFirstWhere = true;
                }
            } else {
                if (!(c instanceof ClauseAssumeClassInitialized)) { //(c instanceof ClauseAssumeClassNotInitialized)
                    expression.append(doneFirstExpression ? " &&" : "");
                    doneFirstExpression = true;
                    expression.append(c.toString());
                }
            }
        }
        if (expression.length() > 0) {
            sb.append(expression);
        }
        if (where.length() > 0) {
            sb.append("where:");
            sb.append(where);
        }
    }

    private static boolean formatExpressionForPathCondition(Expression e, StringBuilder sb, HashSet<String> done) {
        final Primitive firstOp = e.getFirstOperand();
        final Primitive secondOp = e.getSecondOperand();
        boolean someFirstOp = false;
        if (firstOp != null) {
            someFirstOp = formatValueForPathCondition(firstOp, sb, done);
        }
        final StringBuilder second = new StringBuilder();
        final boolean someSecondOp = formatValueForPathCondition(secondOp, second, done);
        if (!someFirstOp || !someSecondOp) {
            //does nothing
        } else {
            sb.append(" &&");
        }
        sb.append(second);
        return (someFirstOp || someSecondOp);
    }

    private static boolean formatValueForPathCondition(Value v, StringBuilder sb, HashSet<String> done) {
        if (v instanceof Expression) {
            return formatExpressionForPathCondition((Expression) v, sb, done);
        } else if (v instanceof PrimitiveSymbolicAtomic || v instanceof ReferenceSymbolicAtomic) {
            if (done.contains(v.toString())) {
                return false;
            } else {
                done.add(v.toString());
                sb.append(v.toString()); sb.append(" == "); sb.append(((Symbolic) v).asOriginString());
                return true;
            }
        } else if (v instanceof PrimitiveSymbolicApply) {
            return formatFunctionApplicationForPathCondition((PrimitiveSymbolicApply) v, sb, done);
        } else if (v instanceof ReferenceSymbolicApply) {
            return formatFunctionApplicationForPathCondition((ReferenceSymbolicApply) v, sb, done);
        } else if (v instanceof WideningConversion) {
            final WideningConversion pWiden = (WideningConversion) v;
            return formatValueForPathCondition(pWiden.getArg(), sb, done);
        } else if (v instanceof NarrowingConversion) {
            final NarrowingConversion pNarrow = (NarrowingConversion) v;
            return formatValueForPathCondition(pNarrow.getArg(), sb, done);
        } else { //(v instanceof DefaultValue || v instanceof Any || v instanceof Simplex || v instanceof Term || v instanceof ReferenceConcrete ||
            // v instanceof ReferenceArrayImmaterial || v instanceof KlassPseudoReference)
            return false;
        }
    }

    private static boolean formatFunctionApplicationForPathCondition(PrimitiveSymbolicApply a, StringBuilder sb, HashSet<String> done) {
        boolean some = false;
        boolean firstDone = false;
        for (Value v : a.getArgs()) {
            final StringBuilder arg = new StringBuilder();
            final boolean argSome = formatValueForPathCondition(v, arg, done);
            some = some || argSome;
            if (argSome) {
                if (firstDone) {
                    sb.append(" &&");
                } else {
                    firstDone = true;
                }
                sb.append(arg);
            }
        }
        return some;
    }

    private static boolean formatFunctionApplicationForPathCondition(ReferenceSymbolicApply a, StringBuilder sb, HashSet<String> done) {
        boolean first = true;
        boolean some = false;
        for (Value v : a.getArgs()) {
            final StringBuilder arg = new StringBuilder();
            final boolean argSome = formatValueForPathCondition(v, arg, done);
            some = some || argSome;
            if (argSome) {
                //does nothing
            } else {
                if (!first) {
                    sb.append(" &&");
                }
                sb.append(arg);
                first = false;
            }
        }
        return some;
    }

    private static void formatValue(State s, StringBuilder sb, Value val) {
        if (val.getType() == Type.CHAR && val instanceof Simplex) {
            final char c = ((Character) ((Simplex) val).getActualValue()).charValue();
            if (c == '\t') {
                sb.append("\\t");
            } else if (c == '\b') {
                sb.append("\\b");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\f') {
                sb.append("\\f");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\"') {
                sb.append("\\\"");
            } else if (c == '\'') {
                sb.append("\\\'");
            } else if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '\u0000') {
                sb.append("\\u0000");
            } else {
                sb.append(c);
            }
        } else {
            sb.append(val.toString());
        }
        if (val instanceof ReferenceSymbolic) {
            final ReferenceSymbolic ref = (ReferenceSymbolic) val;
            if (s.resolved(ref)) {
                if (s.isNull(ref)) {
                    sb.append(" == null");
                } else {
                    sb.append(" == Object["); sb.append(s.getResolution(ref)); sb.append("]");
                }
            }
        }
    }
}

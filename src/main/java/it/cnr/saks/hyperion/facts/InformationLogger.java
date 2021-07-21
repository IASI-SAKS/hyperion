package it.cnr.saks.hyperion.facts;

import it.cnr.saks.hyperion.discovery.Configuration;
import it.cnr.saks.hyperion.symbolic.AnalyzerException;
import jbse.bc.ClassFile;
import jbse.bc.Signature;
import jbse.common.Type;
import jbse.mem.*;
import jbse.mem.exc.FrozenStateException;
import jbse.mem.exc.InvalidNumberOfOperandsException;
import jbse.mem.exc.ThreadStackEmptyException;
import jbse.val.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;

import static jbse.algo.Util.valueString;
import static jbse.bc.Signatures.*;
import static jbse.common.Type.splitParametersDescriptors;

public class InformationLogger {
    private PrintStream datalogOut = null;
    private final Stack<Integer> callerFrame = new Stack<>();
    private Integer invocationEpoch = 0;
    private final List<String> excludePackages;

    // Class -> Method -> Information Data
    private HashMap<String, HashMap<String, TestInformation>> loggedInformation = new HashMap<>();
    private String currClass;
    private String currMethod;

    public InformationLogger(Configuration configuration) {
        this.callerFrame.push(this.invocationEpoch++);
        this.excludePackages = configuration.getExcludeTracedPackages();
    }

    public void resetCounters() {
        this.callerFrame.empty();
        this.invocationEpoch = 0;
        this.callerFrame.push(this.invocationEpoch++);
    }

    public void onThrow(State currentState) {
        final Objekt myException;
        try {
            final Frame frame = currentState.getCurrentFrame();
            final Value[] operands = frame.operands(1); // athrow has one operand
            myException = currentState.getObject((Reference) operands[0]);
        } catch (ThreadStackEmptyException | FrozenStateException | InvalidNumberOfOperandsException e) {
            e.printStackTrace();
            return;
        }

        TestInformation.ExceptionThrown ex = this.loggedInformation.get(this.currClass).get(this.currMethod).addExceptionThrown(myException.getType().getClassName());
    }

    public void onMethodReturn() {
        this.callerFrame.pop();
    }

    public void onMethodCall(State s) throws AnalyzerException {
        Signature callee;
        ClassFile classFile;

        this.callerFrame.push(this.invocationEpoch++);

        if(s.isStuck())
            return;

        int stackFrames = s.getStackSize();
        if(stackFrames < 2) // Skip test program entry point
            return;

        try {
            callee = s.getCurrentMethodSignature();
            classFile = s.getCurrentClass();
        } catch (ThreadStackEmptyException e) {
            e.printStackTrace();
            return;
        }

        String name = callee.getName();
        if(name == null)
            return;

        for(String exclude: this.excludePackages) {
            if(callee.getClassName().startsWith(exclude))
                return;
        }

        Signature caller = null;
        int callerPC = -1;
        try {
             caller = s.getStack().get(stackFrames - 2).getMethodSignature();
             callerPC = s.getStack().get(stackFrames - 2).getProgramCounter();
        } catch (FrozenStateException e) {
            e.printStackTrace();
        }

        String branchId = s.getBranchIdentifier().substring(1);
        String pathId = "[" + branchId.replaceAll("\\.", ", ") + "], " + s.getSequenceNumber();
        String programPoint = caller.getClassName() + ":" + caller.getName() + ":" + caller.getDescriptor();

        this.inspectMethodCall(s, name, callee, classFile, pathId, programPoint, callerPC);
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

    public void emitDatalog() {
        if(this.datalogOut == null)
            return;

        this.loggedInformation.forEach((klass,methodsInKlass) -> { // for each class
            if(methodsInKlass.size() == 0)
                return;

            methodsInKlass.forEach((method, methodLoggedInformation) -> { // for each analyzed method
                // Process method calls
                ArrayList<TestInformation.MethodCall> methodCalls = methodLoggedInformation.getMethodCalls();
                for (TestInformation.MethodCall methodCall : methodCalls) {
                    StringBuilder invokes = new StringBuilder();
                    invokes.append("invokes(").append("'")
                            .append(klass).append(":").append(method).append("', ")
                            .append(methodCall.getPathId()).append(", ")
                            .append("'").append(methodCall.getProgramPoint()).append("', ")
                            .append(methodCall.getCallerPC()).append(", ")
                            .append(methodCall.getCallerEpoch()).append(", ")
                            .append(methodCall.getPathCondition()).append(", ")
                            .append("'").append(methodCall.getClassName()).append(":").append(methodCall.getMethodName()).append(":").append(methodCall.getMethodDescriptor()).append("', ")
                            .append(methodCall.getParameterSet().getParameters())
                            .append(").");

                    this.datalogOut.println(invokes);
                }

                ArrayList<TestInformation.ExceptionThrown> exceptionsThrown = methodLoggedInformation.getExceptionsThrown();
                for(TestInformation.ExceptionThrown ex : exceptionsThrown) {
                    StringBuilder exception = new StringBuilder();
                    exception.append("exception('")
                            .append(klass).append(":").append(method).append("', ")
                            .append("'").append(ex.getExceptionClass()).append("'")
                            .append(")");

                    this.datalogOut.println(exception);
                }
            });
        });

        // Get rid of dumped data from memory
        this.loggedInformation = new HashMap<>();
    }

    private void inspectMethodCall(State s, String name, Signature callee, ClassFile classFile, String pathId, String programPoint, int callerPC) throws AnalyzerException {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        formatPathCondition(s, sb);
        sb.append("]");

        int callerEpoch = this.callerFrame.peek();
        TestInformation.MethodCall md = this.loggedInformation.get(this.currClass).get(this.currMethod).addMethodCall(name, callerEpoch, callee.getDescriptor(), classFile.getClassName(), pathId, programPoint, callerPC, sb.toString());

        int numOperands = splitParametersDescriptors(callee.getDescriptor()).length;
        if(numOperands == 0)
            return;

        SortedMap<Integer, Variable> localVariablesTreeMap = null;
        try {
            localVariablesTreeMap = s.getCurrentFrame().localVariables();

            if(localVariablesTreeMap.get(0).getName().equals("this")) {
                localVariablesTreeMap = localVariablesTreeMap.tailMap(1);
            }

            // We might get a higher count of operands, due to variadic functions
            localVariablesTreeMap = localVariablesTreeMap.headMap(Math.min(localVariablesTreeMap.size(), numOperands) + 1);
        } catch (ThreadStackEmptyException | FrozenStateException e) {
            e.printStackTrace();
        }

        TestInformation.ParameterSet pSet = new TestInformation.ParameterSet();

        // Extract parameter information
        for(Map.Entry<Integer, Variable> v: Objects.requireNonNull(localVariablesTreeMap).entrySet()) {
            Value op = v.getValue().getValue();

            sb = new StringBuilder();

            sb.append("'");
            if (op instanceof Null) {
                sb.append("null");
            } else if(op instanceof ReferenceSymbolicMemberField) {
                ReferenceSymbolicMemberField rsmf = (ReferenceSymbolicMemberField) op;
                final String value = rsmf.getStaticType();
                sb.append(value);
            } else if (op instanceof Simplex) {
                sb.append(renderSimplex((Simplex) op));
            } else if(op instanceof ReferenceConcrete || op instanceof ReferenceSymbolic) {
                try {
                    Objekt obj = s.getObject((Reference) op);
                    if(obj.getType().getSuperclassName().equals(JAVA_ENUM)) {
                        final Signature ordinalSig = new Signature("java/lang/Enum", "I", "ordinal");
                        final int enumMember = Integer.parseInt(obj.getFieldValue(ordinalSig).toString()); // 0 based!
                        final Collection<Signature> fieldSignatures = obj.getAllStoredFieldSignatures();
                        final Signature[] fieldSignaturesArray = fieldSignatures.toArray(new Signature[fieldSignatures.size()]);
                        sb.append(obj.getType().getClassName())
                            .append(".")
                            .append(fieldSignaturesArray[enumMember].getName());
                    } else {
                        switch (obj.getType().getClassName()) {
                            case JAVA_STRING:
                                final Reference valueRef = (Reference) obj.getFieldValue(JAVA_STRING_VALUE);
                                final Array value = (Array) s.getObject(valueRef);
                                sb.append(value.valueString());
                                break;
                            case JAVA_BYTE:
                                sb.append(renderSimplex((Simplex) obj.getFieldValue(JAVA_BYTE_VALUE)));
                                break;
                            case JAVA_DOUBLE:
                                sb.append(renderSimplex((Simplex) obj.getFieldValue(JAVA_DOUBLE_VALUE)));
                                break;
                            case JAVA_FLOAT:
                                sb.append(renderSimplex((Simplex) obj.getFieldValue(JAVA_FLOAT_VALUE)));
                                break;
                            case JAVA_INTEGER:
                                sb.append(renderSimplex((Simplex) obj.getFieldValue(JAVA_INTEGER_VALUE)));
                                break;
                            case JAVA_LONG:
                                sb.append(renderSimplex((Simplex) obj.getFieldValue(JAVA_LONG_VALUE)));
                                break;
                            case JAVA_SHORT:
                                sb.append(renderSimplex((Simplex) obj.getFieldValue(JAVA_SHORT_VALUE)));
                                break;
                            default:
                                sb.append("L");
                                sb.append(obj.getType().getClassName());
                                sb.append(";");
                                break;
                        }
                    }
                } catch (FrozenStateException e) {
                    throw new AnalyzerException("Frozen State while peeking Objext: " + e.getMessage());
                }
            } else
                throw new AnalyzerException("WIP: might have missed some cases...");
            sb.append("'");

            String parm = sb.toString();
            pSet.addParameter(parm);
        }

        md.setParameterSet(pSet);
    }

    private String renderSimplex(Simplex op) {
        String representation = op.toString();
        char type = op.getType();
        if(type == 'B')
            representation = representation.replaceAll("\\(byte\\) ", "");
        if(type == 'S')
            representation = representation.replaceAll("\\(short\\) ", "");
        if(type == 'J')
            representation = representation.replaceAll("L", "");
        if(type == 'F')
            representation = representation.replaceAll("f", "");
        if(type == 'D')
            representation = representation.replaceAll("d", "");
        return representation;
    }

    private static void formatPathCondition(State s, StringBuilder sb) {
        final StringBuilder expression = new StringBuilder();
        boolean doneFirstExpression = false;
        HashSet<String> doneSymbols = new HashSet<>();
        for (Clause c : s.getPathCondition()) {
            if (c instanceof ClauseAssume) {
                expression.append(doneFirstExpression ? ", " : "");
                doneFirstExpression = true;
                final Primitive cond = ((ClauseAssume) c).getCondition();
                expression.append("constr('");
                formatValue(s, expression, cond);
                expression.append("')");
            } else if (c instanceof ClauseAssumeReferenceSymbolic) {
                expression.append(doneFirstExpression ? ", " : "");
                doneFirstExpression = true;
                final ReferenceSymbolic ref = ((ClauseAssumeReferenceSymbolic) c).getReference();
                if (s.isNull(ref)) {
                    expression.append("isNull('");
                    formatValueForPathCondition(ref, expression, doneSymbols);
                    expression.append("')");
                } else {
                    expression.append("pointsTo('");
                    formatValueForPathCondition(ref, expression, doneSymbols);
                    expression.append("', ")
                            .append("'Object[")
                            .append(s.getResolution(ref))
                            .append("]')");
                }
            } else {
                if (!(c instanceof ClauseAssumeClassInitialized)) {
                    expression.append(doneFirstExpression ? ", " : "");
                    doneFirstExpression = true;
                    expression.append(c.toString());
                }
            }
        }
        if (expression.length() > 0) {
            sb.append(expression);
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
            sb.append(", ");
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
                if(v instanceof ReferenceSymbolicLocalVariable)
                    sb.append(((ReferenceSymbolicLocalVariable) v).getVariableName());
                else if(v instanceof PrimitiveSymbolicMemberField)
                    sb.append(((PrimitiveSymbolicMemberField) v).getFieldName());
                else
                    sb.append(((Symbolic) v).asOriginString());
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
                    sb.append("), ");
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
                    sb.append(", ");
                }
                sb.append(arg);
                first = false;
            }
        }
        return some;
    }

    private static void formatValue(State s, StringBuilder sb, Value val) {
        if (val.getType() == Type.CHAR && val instanceof Simplex) {
            final char c = (Character)((Simplex) val).getActualValue();
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
                sb.append("\\'");
            } else if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '\u0000') {
                sb.append("\\u0000");
            } else {
                sb.append(c);
            }
        } else {
            if(val instanceof Expression)
                sb.append(((Expression)val).asOriginString());
            else
                sb.append(val);
        }

        // TODO: recheck this part
        if (val instanceof ReferenceSymbolic) {
            final ReferenceSymbolic ref = (ReferenceSymbolic) val;
            if (s.resolved(ref)) {
                if (s.isNull(ref)) {
                    sb.append(" == null");
                } else {
                    sb.append(" == Object[");
                    sb.append(s.getResolution(ref));
                    sb.append("]");
                }
            }
        }
    }

    private static class ValueClassMentionDetector implements ValueVisitor {
        private Map.Entry<ClassFile, Klass> e;
        private boolean mentionsClass;

        ValueClassMentionDetector(Map.Entry<ClassFile, Klass> e) {
            this.e = e;
        }

        boolean mentionsClass() {
            return this.mentionsClass;
        }

        @Override
        public void visitAny(Any x) {
            this.mentionsClass = false;
        }

        @Override
        public void visitExpression(Expression e) {
            try {
                if (e.isUnary()) {
                    e.getOperand().accept(this);
                } else {
                    e.getFirstOperand().accept(this);
                    if (!this.mentionsClass) {
                        e.getSecondOperand().accept(this);
                    }
                }
            } catch (RuntimeException exc) {
                throw exc;
            } catch (Exception exc) {
                //cannot happen;
            }
        }

        @Override
        public void visitPrimitiveSymbolicApply(PrimitiveSymbolicApply x) {
            try {
                for (Value v : x.getArgs()) {
                    v.accept(this);
                    if (this.mentionsClass) {
                        return;
                    }
                }
            } catch (RuntimeException exc) {
                throw exc;
            } catch (Exception exc) {
                //cannot happen;
            }
        }

        @Override
        public void visitPrimitiveSymbolicAtomic(PrimitiveSymbolicAtomic s) {
            this.mentionsClass = false;
        }

        @Override
        public void visitSimplex(Simplex x) {
            this.mentionsClass = false;
        }

        @Override
        public void visitTerm(Term x) throws Exception {
            this.mentionsClass = false;
        }

        @Override
        public void visitNarrowingConversion(NarrowingConversion x) {
            try {
                x.getArg().accept(this);
            } catch (RuntimeException exc) {
                throw exc;
            } catch (Exception exc) {
                //cannot happen;
            }
        }

        @Override
        public void visitWideningConversion(WideningConversion x) {
            try {
                x.getArg().accept(this);
            } catch (RuntimeException exc) {
                throw exc;
            } catch (Exception exc) {
                //cannot happen;
            }
        }

        @Override
        public void visitReferenceArrayImmaterial(ReferenceArrayImmaterial x) {
            //the class of the immaterial reference is not displayed, but we
            //save it nevertheless
            this.mentionsClass = (x.getArrayType().equals(this.e.getKey()));
        }

        @Override
        public void visitReferenceConcrete(ReferenceConcrete x) {
            this.mentionsClass = false;
        }

        @Override
        public void visitKlassPseudoReference(KlassPseudoReference x) {
            this.mentionsClass = (x.getClassFile().equals(this.e.getKey()));
        }

        @Override
        public void visitReferenceSymbolicApply(ReferenceSymbolicApply x) {
            try {
                for (Value v : x.getArgs()) {
                    v.accept(this);
                    if (this.mentionsClass) {
                        return;
                    }
                }
            } catch (RuntimeException exc) {
                throw exc;
            } catch (Exception exc) {
                //cannot happen;
            }
        }

        @Override
        public void visitReferenceSymbolicLocalVariable(ReferenceSymbolicLocalVariable x) {
            this.mentionsClass = false;
        }

        @Override
        public void visitReferenceSymbolicMemberArray(ReferenceSymbolicMemberArray x) {
            try {
                x.getContainer().accept(this);
            } catch (RuntimeException exc) {
                throw exc;
            } catch (Exception exc) {
                //cannot happen;
            }
        }

        @Override
        public void visitReferenceSymbolicMemberField(ReferenceSymbolicMemberField x) {
            try {
                x.getContainer().accept(this);
            } catch (RuntimeException exc) {
                throw exc;
            } catch (Exception exc) {
                //cannot happen;
            }
        }

        @Override
        public void visitReferenceSymbolicMemberMapKey(ReferenceSymbolicMemberMapKey x) {
            try {
                x.getContainer().accept(this);
            } catch (RuntimeException exc) {
                throw exc;
            } catch (Exception exc) {
                //cannot happen;
            }
        }

        @Override
        public void visitReferenceSymbolicMemberMapValue(ReferenceSymbolicMemberMapValue x) {
            try {
                x.getContainer().accept(this);
            } catch (RuntimeException exc) {
                throw exc;
            } catch (Exception exc) {
                //cannot happen;
            }
        }

        @Override
        public void visitDefaultValue(DefaultValue x) {
            this.mentionsClass = false;
        }
    }
}

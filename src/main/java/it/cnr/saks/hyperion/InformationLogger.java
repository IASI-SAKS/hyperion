package it.cnr.saks.hyperion;

import jbse.bc.ClassFile;
import jbse.bc.Signature;
import jbse.common.Type;
import jbse.mem.*;
import jbse.mem.exc.FrozenStateException;
import jbse.mem.exc.ThreadStackEmptyException;
import jbse.val.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;

import static jbse.algo.Util.valueString;
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

    public void onMethodReturn() {
        this.callerFrame.pop();
    }

    public void onMethodCall(State s) {
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

//        System.out.println(name);

//        if((name.equals("get") || name.equals("post") || name.equals("put") || name.equals("delete") )
//                && classFile.getClassName().equals("org/springframework/test/web/servlet/request/MockMvcRequestBuilders")) {
//            this.inspectHttpRequest(s, name, pathId);
//        }

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

//        this.datalogOut.println("FORMATO:\n\ninvokes("
//                + "\"nome\" del test, "
//                + "branch point, sequence number" + ", " + "caller" + ", " + "path condition" + ", "
//                + "metodo chiamato" + ", "
//                + "parametri" + ").\n\n");
//        Formato:
//        invokes(test name, branch point, branch sequence number, caller, callerPC, frameEpoch, path condition, callee, parameters)

        this.loggedInformation.forEach((klass,methodsInKlass) -> { // for each class
            if(methodsInKlass.size() == 0)
                return;

            methodsInKlass.forEach((method, methodLoggedInformation) -> { // for each analyzed method
                // Process method calls
                ArrayList<TestInformation.MethodCall> methodCalls = methodLoggedInformation.getMethodCalls();
                for (TestInformation.MethodCall methodCall : methodCalls) {
                    StringBuilder invokes = new StringBuilder();
                    invokes.append("invokes(")
                            .append("'" + klass + ":" + method + "', ")
                            .append(methodCall.getPathId() + ", ")
                            .append("'" + methodCall.getProgramPoint() + "', ")
                            .append(methodCall.getCallerPC() + ", ")
                            .append(methodCall.getCallerEpoch() + ", ")
                            .append(methodCall.getPathCondition() + ", ")
                            .append("'" + methodCall.getClassName() + ":" + methodCall.getMethodName() + ":" + methodCall.getMethodDescriptor() + "', ")
                            .append(methodCall.getParameterSet().getParameters())
                            .append(").");

                    this.datalogOut.println(invokes);
                }
            });
        });

        // Get rid of dumped data from memory
        this.loggedInformation = new HashMap<>();
    }

    private void inspectMethodCall(State s, String name, Signature callee, ClassFile classFile, String pathId, String programPoint, int callerPC) {
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
            localVariablesTreeMap = localVariablesTreeMap.tailMap(localVariablesTreeMap.size() - numOperands);
        } catch (ThreadStackEmptyException | FrozenStateException e) {
            e.printStackTrace();
        }

        TestInformation.ParameterSet pSet = new TestInformation.ParameterSet();

        for(Map.Entry<Integer, Variable> v: Objects.requireNonNull(localVariablesTreeMap).entrySet()) {
            Value op = v.getValue().getValue();

            sb = new StringBuilder();

            if(op instanceof ReferenceSymbolicMemberField) {
                ReferenceSymbolicMemberField rsmf = (ReferenceSymbolicMemberField)op;
                final String value = rsmf.getValue();

                Set<Map.Entry<ClassFile, Klass>> entries = null;
                try {
                    final Map<ClassFile, Klass> a = s.getStaticMethodArea();
                    entries = a.entrySet();
                } catch (FrozenStateException e) {
                    e.printStackTrace();
                }
            }

            if (op instanceof Simplex) {
                String representation = op.toString();
                if(v.getValue().getType().equals("B"))
                    representation = representation.replaceAll("\\(byte\\) ", "");
                if(v.getValue().getType().equals("J"))
                    representation = representation.replaceAll("L", "");
                if(v.getValue().getType().equals("F"))
                    representation = representation.replaceAll("f", "");
                sb.append(representation);
            } else if(op.isSymbolic()) {
                sb.append("'");
                formatValueForPathCondition(op, sb, new HashSet<>());
                sb.append("'");
            } else if(op instanceof Reference) {
                try {
                    HeapObjekt obj = s.getObject((Reference) op);
                    if (obj == null) {
                        sb.append("null");
                    } else {
                        if (obj.getType().getClassName().equals("java/lang/String")) {
                            sb.append("'")
                              .append(valueString(s, (Instance) obj))
                              .append("'");
                        } else {
                            sb.append("'")
                              .append(op)
                              .append("'");
                        }
                    }
                } catch (FrozenStateException e) {
                    e.printStackTrace();
                }
            } else {
                sb.append(op.getClass().toString());
            }

            String parm = sb.toString();
            pSet.addParameter(parm);
        }

        md.setParameterSet(pSet);
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

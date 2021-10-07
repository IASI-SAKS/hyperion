package it.cnr.saks.hyperion.similarity;

import java.util.ArrayList;

public class TestInformation {
    private final ArrayList<MethodCall> methodCalls = new ArrayList<>();
    private final ArrayList<ExceptionThrown> exceptionsThrown = new ArrayList<>();
    private final String testClass;
    private final String testMethod;

    public TestInformation(String className, String methodName) {
        this.testClass = className;
        this.testMethod = methodName;
    }

    protected MethodCall addMethodCall(String methodName, int callerEpoch, String methodDescriptor, String className, String pathId, String programPoint, int callerPC, String pathCondition) {
        MethodCall md = new MethodCall(methodName, callerEpoch, methodDescriptor, className, pathId, programPoint, callerPC, pathCondition);
        this.methodCalls.add(md);
        return md;
    }

    public ArrayList<MethodCall> getMethodCalls() {
        return this.methodCalls;
    }

    protected ExceptionThrown addExceptionThrown(String exceptionClass) {
        ExceptionThrown ex = new ExceptionThrown(exceptionClass);
        this.exceptionsThrown.add(ex);
        return ex;
    }

    public ArrayList<ExceptionThrown> getExceptionsThrown() {
        return this.exceptionsThrown;
    }

    protected static class ExceptionThrown {
        private final String exceptionClass;

        public ExceptionThrown(String exceptionClass) {
            this.exceptionClass = exceptionClass;
        }

        public String getExceptionClass() {
            return this.exceptionClass;
        }
    }

    public String getTestClass() {
        return testClass;
    }

    public String getTestMethod() {
        return testMethod;
    }

    protected static class MethodCall {
        private final String methodName;
        private final int callerEpoch;
        private final String methodDescriptor;
        private final String className;
        private final String pathId;
        private final String programPoint;
        private final String pathCondition;
        private final int callerPC;
        private ParameterSet parameters = new ParameterSet();


        public MethodCall(String methodName, int callerEpoch, String methodDescriptor, String className, String pathId, String programPoint, int callerPC, String pathCondition) {
            this.methodName = methodName;
            this.callerEpoch = callerEpoch;
            this.methodDescriptor = methodDescriptor;
            this.className = className;
            this.pathId = pathId;
            this.programPoint = programPoint;
            this.callerPC = callerPC;
            this.pathCondition = pathCondition;
        }

        public String getClassName() {
            return className;
        }

        public String getMethodDescriptor() {
            return methodDescriptor;
        }

        public int getCallerEpoch() {
            return callerEpoch;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getPathId() {
            return pathId;
        }

        public String getProgramPoint() {
            return programPoint;
        }

        public String getPathCondition() {
            return pathCondition;
        }

        public int getCallerPC() {
            return callerPC;
        }

        public ParameterSet getParameterSet() {
            return parameters;
        }

        public void setParameterSet(ParameterSet parameters) {
            this.parameters = parameters;
        }
    }

    protected static class ParameterSet {
        private final ArrayList<String> parameters = new ArrayList<>();

        public void addParameter(String s) {
            this.parameters.add(s);
        }

        public String getParameters() {
            StringBuilder ret = new StringBuilder();
            ret.append("[");
            boolean doneFirst = false;
            for(String s: parameters) {
                ret.append(doneFirst ? ", " : "");
                doneFirst = true;
                ret.append(s);
            }
            ret.append("]");
            return ret.toString();
        }
    }
}
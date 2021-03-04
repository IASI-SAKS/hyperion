package it.cnr.saks.hyperion;

import java.util.ArrayList;

public class TestInformation {
    private final ArrayList<MethodCall> methodCalls = new ArrayList<>();
    private final ArrayList<EndPoint> endPoints = new ArrayList<>();
    private final ArrayList<String> exceptions = new ArrayList<>();

    protected MethodCall addMethodCall(String methodName, int callerEpoch, String methodDescriptor, String className, String pathId, String programPoint, int callerPC, String pathCondition) {
        for(MethodCall md : this.methodCalls) {
            if (md.getMethodDescriptor().equals(methodDescriptor) && md.getClassName().equals(className) && md.getProgramPoint().equals(programPoint)) {
                return md;
            }
        }

        MethodCall md = new MethodCall(methodName, callerEpoch, methodDescriptor, className, pathId, programPoint, callerPC, pathCondition);
        this.methodCalls.add(md);
        return md;
    }

    protected void addEndPoint(String type, String endPoint, String pathId) {
        for(EndPoint ep : this.endPoints) {
            if(ep.getEndPoint().equals(endPoint) && ep.getPathId().equals(pathId)) {
                return;
            }
        }

        EndPoint ep = new EndPoint(type, endPoint, null, pathId);
        this.endPoints.add(ep);
    }

    protected void addException(String exception) {
        this.exceptions.add(exception);
    }

    public ArrayList<MethodCall> getMethodCalls() {
        return this.methodCalls;
    }

    public ArrayList<EndPoint> getEndPoints() {
        return this.endPoints;
    }

    public ArrayList<String> getExceptions() {
        return this.exceptions;
    }


    protected static class EndPoint {
        private final String type;
        private final String endPoint;
        private final String args;
        private final String pathId;

        private EndPoint(String type, String endPoint, String args, String pathId) {
            this.type = type;
            this.endPoint = endPoint;
            this.args = args;
            this.pathId = pathId;
        }

        public String getType() {
            return type;
        }

        public String getEndPoint() {
            return endPoint;
        }

        public String getArgs() {
            return args;
        }

        public String getPathId() {
            return pathId;
        }

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
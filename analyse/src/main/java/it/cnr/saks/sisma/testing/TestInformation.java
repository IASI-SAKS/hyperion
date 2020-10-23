package it.cnr.saks.sisma.testing;

import java.util.ArrayList;
import java.util.Arrays;

public class TestInformation {
    private final ArrayList<MethodCall> methodCalls = new ArrayList<>();
    private final ArrayList<EndPoint> endPoints = new ArrayList<>();
    private final ArrayList<String> exceptions = new ArrayList<>();

    protected MethodCall addMethodCall(String methodName, String methodDescriptor, String className, String pathId, String programPoint, String pathCondition) {
        for(MethodCall md : this.methodCalls) {
            // TODO: siccome ora considero anche i parametri, probabilmente qui è necessario creare entry duplicate...
            if (md.getMethodDescriptor().equals(methodDescriptor) && md.getClassName().equals(className) && md.getPathId().equals(pathId)) {
                return null;
            }
        }

        MethodCall md = new MethodCall(methodName, methodDescriptor, className, pathId, programPoint, pathCondition);
        this.methodCalls.add(md);
        return md;
    }

    protected void addEndPoint(String type, String endPoint, String args, String pathId) {
        for(EndPoint ep : this.endPoints) {
            if(ep.getEndPoint().equals(endPoint) && ep.getPathId().equals(pathId)) {
                return;
            }
        }

        EndPoint ep = new EndPoint(type, endPoint, args, pathId);
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
        private final String methodDescriptor;
        private final String className;
        private final String pathId;
        private final String programPoint;

        private final String pathCondition;
        private ParameterSet parameters = new ParameterSet();


        public MethodCall(String methodName, String methodDescriptor, String className, String pathId, String programPoint, String pathCondition) {
            this.methodName = methodName;
            this.methodDescriptor = methodDescriptor;
            this.className = className;
            this.pathId = pathId;
            this.programPoint = programPoint;
            this.pathCondition = pathCondition;
        }

        public String getClassName() {
            return className;
        }

        public String getMethodDescriptor() {
            return methodDescriptor;
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
            for(String s: parameters) {
                ret.append(s + ", ");
            }
            ret.append("]");
            return ret.toString();
        }
    }

}
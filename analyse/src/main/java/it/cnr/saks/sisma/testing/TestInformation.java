package it.cnr.saks.sisma.testing;

import java.lang.reflect.Method;
import java.util.HashSet;

public class TestInformation {
    private final HashSet<MethodCall> methodCalls = new HashSet<>();;
    private final HashSet<EndPoint> endPoints = new HashSet<>();;
    private final HashSet<String> exceptions = new HashSet<>();;

    protected void addMethodCall(String methodName, String methodDescriptor, String className) {
        for(MethodCall md : this.methodCalls) {
            if (md.getMethodDescriptor().equals(methodDescriptor) && md.getClassName().equals(className)) {
                md.hit();
                return;
            }
        }
        MethodCall md = new MethodCall(null, methodName, methodDescriptor, className);
        this.methodCalls.add(md);
    }

    protected void addEndPoint(String type, String endPoint, String args) {
        EndPoint ep = new EndPoint(type, endPoint, args);
        this.endPoints.add(ep);
    }

    protected void addException(String exception) {
        this.exceptions.add(exception);
    }

    protected HashSet<MethodCall> getMethodCalls() {
        return this.methodCalls;
    }

    protected HashSet<EndPoint> getEndPoints() {
        return this.endPoints;
    }

    protected HashSet<String> getExceptions() {
        return this.exceptions;
    }


    protected class EndPoint {
        private final String type;
        private final String endPoint;
        private final String args;

        private EndPoint(String type, String endPoint, String args) {
            this.type = type;
            this.endPoint = endPoint;
            this.args = args;
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
    }


    protected class MethodCall {
        private final String methodName;
        private final String methodDescriptor;
        private final String className;
        private int hits;


        public MethodCall(Method method, String methodName, String methodDescriptor, String className) {
            this.methodName = methodName;
            this.methodDescriptor = methodDescriptor;
            this.className = className;
            this.hits = 0;
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

        public void hit() {
            this.hits++;
        }

        public int getHits() {
            return this.hits;
        }
    }
}
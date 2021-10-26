package it.cnr.saks.hyperion.discovery;

import java.lang.reflect.Method;

public class MethodDescriptor {
    private String methodName = null;
    private String methodDescriptor = null;
    private String className = null;

    private MethodDescriptor() {}

    public MethodDescriptor(String methodName, String methodDescriptor, String className) {
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodDescriptor() {
        return methodDescriptor;
    }

    public void setMethodDescriptor(String methodDescriptor) {
        this.methodDescriptor = methodDescriptor;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
package it.cnr.saks.hyperion;

import java.lang.reflect.Method;

public class MethodDescriptor {
    private final Method method;
    private final String methodName;
    private final String methodDescriptor;
    private final String className;

    public MethodDescriptor(Method method, String methodName, String methodDescriptor, String className) {
        this.method = method;
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
        this.className = className;
    }

    public Method getMethod() {
        return method;
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
}
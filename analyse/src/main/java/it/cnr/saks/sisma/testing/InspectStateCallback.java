package it.cnr.saks.sisma.testing;

import jbse.apps.run.Callback;
import jbse.mem.State;
import jbse.mem.exc.ThreadStackEmptyException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;

public class InspectStateCallback implements Callback {
    private PrintStream out = null;
    // Class -> Method -> Called Methods
    private HashMap<String, HashMap<String, HashSet<String>>> methodCalls = new HashMap<>();
    private String currClass;
    private String currMethod;

    //public InspectStateCallback() {}

    public void inspectState(State s) {
        try {
            this.methodCalls.get(this.currClass).get(this.currMethod).add(s.getCurrentMethodSignature() + " from class " + s.getCurrentClass());
        } catch (ThreadStackEmptyException e) {}
    }

    public void setOutputFile(String f) {
        final File file = new File(f);
        try {
            this.out = new PrintStream(file);
        } catch (FileNotFoundException e) { }
    }

    public void setCurrClass(String currClass) {
        this.currClass = currClass;
        methodCalls.put(currClass, new HashMap<>());
    }

    public void setCurrMethod(String currMethod) {
        this.currMethod = currMethod;
        methodCalls.get(this.currClass).put(currMethod, new HashSet<>());
    }

    public void dump() {
        methodCalls.forEach((key,value) -> {
            if(value.size() == 0)
                return;
            this.out.println("Class: " + key);
            value.forEach((key2, value2) -> {
                this.out.println("\t Method: " + key2);
                value2.forEach((el) -> this.out.println("\t\t" + el));
            });
        });
    }
}

package it.cnr.saks.sisma.testing;

import jbse.apps.run.Callback;
import jbse.mem.State;
import jbse.mem.exc.ThreadStackEmptyException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

public class InspectStateCallback implements Callback {
    private PrintStream out;

    public InspectStateCallback(String f) {
        final File file = new File(f);
        try {
            this.out = new PrintStream(file);
        } catch (FileNotFoundException e) { }
    }

    public void inspectState(State s) {
        try {
            this.out.println("Called " + s.getCurrentMethodSignature() + " in class " + s.getCurrentClass());
        } catch (ThreadStackEmptyException e) {}
    }
}

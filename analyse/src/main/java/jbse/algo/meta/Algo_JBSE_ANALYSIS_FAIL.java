package jbse.algo.meta;

import jbse.algo.Algo_INVOKEMETA_Nonbranching;
import jbse.jvm.exc.FailureException;
import jbse.mem.State;

import java.util.function.Supplier;

public final class Algo_JBSE_ANALYSIS_FAIL extends Algo_INVOKEMETA_Nonbranching {
    @Override
    protected Supplier<Integer> numOperands() {
        return () -> 0;
    }
    
    @Override
    protected void update(State state) throws FailureException {
        throw new FailureException();
    }
}

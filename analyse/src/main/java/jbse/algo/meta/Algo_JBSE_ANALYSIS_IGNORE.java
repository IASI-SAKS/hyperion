package jbse.algo.meta;

import jbse.algo.Algo_INVOKEMETA_Nonbranching;
import jbse.mem.State;
import jbse.mem.exc.ContradictionException;

import java.util.function.Supplier;

public final class Algo_JBSE_ANALYSIS_IGNORE extends Algo_INVOKEMETA_Nonbranching {
    @Override
    protected Supplier<Integer> numOperands() {
        return () -> 0;
    }
    
    @Override
    protected void update(State state) throws ContradictionException {
        if (state.mayViolateAssumption()) {
            throw new ContradictionException();
        }
    }
}

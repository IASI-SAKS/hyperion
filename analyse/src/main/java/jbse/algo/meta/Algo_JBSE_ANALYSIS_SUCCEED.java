package jbse.algo.meta;

import jbse.algo.Algo_INVOKEMETA_Nonbranching;
import jbse.mem.State;

import java.util.function.Supplier;

public final class Algo_JBSE_ANALYSIS_SUCCEED extends Algo_INVOKEMETA_Nonbranching {
    @Override
    protected Supplier<Integer> numOperands() {
        return () -> 0;
    }
    
    @Override
    protected void update(State state) {
        state.setStuckStop();
    }
}

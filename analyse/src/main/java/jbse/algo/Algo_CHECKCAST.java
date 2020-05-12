package jbse.algo;

import jbse.mem.State;
import jbse.mem.exc.ThreadStackEmptyException;

import static jbse.algo.Util.*;
import static jbse.bc.Signatures.CLASS_CAST_EXCEPTION;

/**
 * {@link Algorithm} implementing the checkcast bytecode.
 *  
 * @author Pietro Braione
 */
final class Algo_CHECKCAST extends Algo_CASTINSTANCEOF {
    @Override
    protected void complete(State state, boolean isSubclass) 
    throws InterruptException {
        if (isSubclass) {
            try {
                state.pushOperand(this.data.operand(0));
            } catch (ThreadStackEmptyException e) {
                //this should never happen
                failExecution(e);
            }
        } else {
            throwNew(state, CLASS_CAST_EXCEPTION);
            exitFromAlgorithm();
        }
    }
}
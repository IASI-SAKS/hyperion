package jbse.algo;

import jbse.mem.State;
import jbse.mem.exc.ThreadStackEmptyException;
import jbse.val.Reference;

import static jbse.algo.Util.*;

/**
 * {@link Algorithm} implementing the instanceof bytecode.
 *  
 * @author Pietro Braione
 */
final class Algo_INSTANCEOF extends Algo_CASTINSTANCEOF {
    @Override
    protected void complete(State state, boolean isSubclass)
    throws InterruptException {
        try {
            final Reference tmpValue = (Reference) this.data.operand(0);
            if (!state.isNull(tmpValue) && isSubclass) { //note that null is not an instance of anything
                state.pushOperand(state.getCalculator().valInt(1));
            } else { 
                state.pushOperand(state.getCalculator().valInt(0));
            }
        } catch (ClassCastException e) {
            throwVerifyError(state);
            exitFromAlgorithm();
        } catch (ThreadStackEmptyException e) {
            //this should never happen
            failExecution(e);
        }
    }
}

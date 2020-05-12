package jbse.algo.meta;

import jbse.algo.Algo_INVOKEMETA_Nonbranching;
import jbse.algo.InterruptException;
import jbse.algo.exc.SymbolicValueNotAllowedException;
import jbse.bc.exc.ClassFileIllFormedException;
import jbse.common.exc.ClasspathException;
import jbse.dec.exc.DecisionException;
import jbse.mem.State;
import jbse.mem.exc.ThreadStackEmptyException;
import jbse.val.Reference;

import java.util.function.Supplier;

import static jbse.algo.Util.*;

/**
 * Meta-level implementation of {@link String#intern()}.
 * 
 * @author Pietro Braione
 */
public final class Algo_JAVA_STRING_INTERN extends Algo_INVOKEMETA_Nonbranching {
    private String valueString; //set by cookMore
    
    @Override
    protected Supplier<Integer> numOperands() {
        return () -> 1;
    }
    
    @Override
    protected void cookMore(State state)
    throws ThreadStackEmptyException, DecisionException,
    ClasspathException, SymbolicValueNotAllowedException,
    InterruptException {
        try {
            this.valueString = valueString(state, (Reference) this.data.operand(0));
            if (this.valueString == null) {
                //TODO remove this limitation
                throw new SymbolicValueNotAllowedException("Cannot intern a symbolic String object.");
            }
            if (state.hasStringLiteral(this.valueString)) {
                //nothing to do
            } else {
                ensureStringLiteral(state, this.valueString, this.ctx);
            }
        } catch (ClassCastException | ClassFileIllFormedException e) {
            throwVerifyError(state);
            exitFromAlgorithm();
        }
    }
    
    @Override
    protected void update(State state) throws ThreadStackEmptyException {
        state.pushOperand(state.referenceToStringLiteral(this.valueString));
    }
}

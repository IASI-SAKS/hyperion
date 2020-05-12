package jbse.algo;

import jbse.dec.DecisionProcedureAlgorithms;
import jbse.mem.Array;
import jbse.tree.DecisionAlternative_NONE;
import jbse.val.Reference;

import java.util.function.Supplier;

import static jbse.algo.Util.*;
import static jbse.bc.Offsets.ARRAYLENGTH_OFFSET;
import static jbse.bc.Signatures.NULL_POINTER_EXCEPTION;

/**
 * {@link Algorithm} implementing the arraylength bytecode.
 * 
 * @author Pietro Braione
 */
final class Algo_ARRAYLENGTH extends Algorithm<
        BytecodeData_0,
DecisionAlternative_NONE,
StrategyDecide<DecisionAlternative_NONE>,
        StrategyRefine<DecisionAlternative_NONE>,
        StrategyUpdate<DecisionAlternative_NONE>> {
    
    @Override
    protected Supplier<Integer> numOperands() {
        return () -> 1;
    }
    
    @Override
    protected Supplier<BytecodeData_0> bytecodeData() {
        return BytecodeData_0::get;
    }
	
    @Override
    protected BytecodeCooker bytecodeCooker() {
        return (state) -> { };
    }
    
    @Override
    protected Class<DecisionAlternative_NONE> classDecisionAlternative() {
        return DecisionAlternative_NONE.class;
    }

    @Override
    protected StrategyDecide<DecisionAlternative_NONE> decider() {
        return (state, result) -> { 
            result.add(DecisionAlternative_NONE.instance());
            return DecisionProcedureAlgorithms.Outcome.FF;
        };
    }
    
    @Override
    protected StrategyRefine<DecisionAlternative_NONE> refiner() {
        return (state, alt) -> { };
    }

    @Override
    protected StrategyUpdate<DecisionAlternative_NONE> updater() {
        return (state, alt) -> { 
            try {
                final Reference tmpRef = (Reference) this.data.operand(0);
                if (state.isNull(tmpRef)) {
                    throwNew(state, NULL_POINTER_EXCEPTION);
                    exitFromAlgorithm();
                }
                final Array tmpArray = (Array) state.getObject(tmpRef);
                state.pushOperand(tmpArray.getLength());
            } catch (ClassCastException e) {
                throwVerifyError(state);
                exitFromAlgorithm();
            }
        };
    }
    
    @Override
    protected final Supplier<Boolean> isProgramCounterUpdateAnOffset() {
        return () -> true;
    }
    
    @Override
    protected final Supplier<Integer> programCounterUpdate() {
        return () -> ARRAYLENGTH_OFFSET;
    }
}

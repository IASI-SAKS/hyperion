package jbse.algo;

import jbse.bc.exc.BadClassFileException;
import jbse.bc.exc.ClassFileNotAccessibleException;
import jbse.bc.exc.ClassFileNotFoundException;
import jbse.mem.State;
import jbse.mem.exc.ThreadStackEmptyException;
import jbse.val.Primitive;

import java.util.function.Supplier;

import static jbse.algo.Util.*;
import static jbse.bc.Offsets.ANEWARRAY_OFFSET;
import static jbse.bc.Signatures.ILLEGAL_ACCESS_ERROR;
import static jbse.bc.Signatures.NO_CLASS_DEFINITION_FOUND_ERROR;
import static jbse.common.Type.*;

/**
 * Algorithm managing the anewarray bytecode.
 * 
 * @author Pietro Braione
 */
final class Algo_ANEWARRAY extends Algo_XNEWARRAY<BytecodeData_1CL> {

    @Override
    protected Supplier<Integer> numOperands() {
        return () -> 1;
    }

    @Override
    protected Supplier<BytecodeData_1CL> bytecodeData() {
        return BytecodeData_1CL::get;
    }

    @Override
    protected void preCook(State state) throws InterruptException {
        //sets the array length
        try {
            this.dimensionsCounts = new Primitive[] { (Primitive) this.data.operand(0) };
        } catch (ClassCastException e) {
            throwVerifyError(state);
            exitFromAlgorithm();
        }

        //sets the array type
        this.arrayType = "" + ARRAYOF + REFERENCE + this.data.className() + TYPEEND;

        //resolves the member class
        try {
            final String currentClassName = state.getCurrentMethodSignature().getClassName();
            state.getClassHierarchy().resolveClass(currentClassName, this.data.className());
        } catch (ClassFileNotFoundException e) {
            throwNew(state, NO_CLASS_DEFINITION_FOUND_ERROR);
            exitFromAlgorithm();
        } catch (ClassFileNotAccessibleException e) {
            throwNew(state, ILLEGAL_ACCESS_ERROR);
            exitFromAlgorithm();
        } catch (BadClassFileException e) {
            throwVerifyError(state);
            exitFromAlgorithm();
        } catch (ThreadStackEmptyException e) {
            //this should never happen
            failExecution(e);
        }
    }

    @Override
    protected Supplier<Integer> programCounterUpdate() {
        return () -> ANEWARRAY_OFFSET;
    }
}
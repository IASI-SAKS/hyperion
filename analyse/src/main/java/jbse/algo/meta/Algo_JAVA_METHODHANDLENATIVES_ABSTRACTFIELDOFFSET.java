package jbse.algo.meta;

import static jbse.algo.Util.exitFromAlgorithm;
import static jbse.algo.Util.throwNew;
import static jbse.algo.Util.valueString;
import static jbse.algo.meta.Util.FAIL_JBSE;
import static jbse.algo.meta.Util.getInstance;
import static jbse.algo.meta.Util.INTERRUPT_SYMBOLIC_VALUE_NOT_ALLOWED_EXCEPTION;
import static jbse.algo.meta.Util.IS_FIELD;
import static jbse.bc.Signatures.INTERNAL_ERROR;
import static jbse.common.Type.LONG;
import static jbse.bc.Signatures.JAVA_MEMBERNAME_CLAZZ;
import static jbse.bc.Signatures.JAVA_MEMBERNAME_FLAGS;
import static jbse.bc.Signatures.JAVA_MEMBERNAME_NAME;
import static jbse.bc.Signatures.JAVA_MEMBERNAME_TYPE;

import java.lang.reflect.Modifier;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import jbse.algo.Algo_INVOKEMETA_Nonbranching;
import jbse.algo.InterruptException;
import jbse.algo.StrategyUpdate;
import jbse.algo.exc.SymbolicValueNotAllowedException;
import jbse.algo.meta.Util.ErrorAction;
import jbse.bc.ClassFile;
import jbse.bc.Signature;
import jbse.common.exc.ClasspathException;
import jbse.common.exc.InvalidInputException;
import jbse.mem.Instance;
import jbse.mem.Instance_JAVA_CLASS;
import jbse.mem.State;
import jbse.tree.DecisionAlternative_NONE;
import jbse.val.Reference;
import jbse.val.Simplex;
import jbse.val.exc.InvalidTypeException;

/**
 * Meta-level abstract implementation of {@link java.lang.invoke.MethodHandleNatives#objectFieldOffset(java.lang.invoke.MemberName)}
 * and {@link java.lang.invoke.MethodHandleNatives#staticFieldOffset(java.lang.invoke.MemberName)}
 * 
 * @author Pietro Braione
 */
abstract class Algo_JAVA_METHODHANDLENATIVES_ABSTRACTFIELDOFFSET extends Algo_INVOKEMETA_Nonbranching {
	private final boolean must_be_static;
	private Simplex ofst; //set by cookMore
	
	protected Algo_JAVA_METHODHANDLENATIVES_ABSTRACTFIELDOFFSET(boolean must_be_static) {
		this.must_be_static = must_be_static;
	}
	
	@Override
	protected Supplier<Integer> numOperands() {
		return () -> 1;
	}

	@Override
	protected void cookMore(State state) 
	throws InterruptException, SymbolicValueNotAllowedException, ClasspathException, 
	InvalidInputException, NoSuchElementException, InvalidTypeException {
		final ErrorAction THROW_JAVA_INTERNAL_ERROR = msg -> { throwNew(state, this.ctx.getCalculator(), INTERNAL_ERROR); exitFromAlgorithm(); };

		//checks the first parameter (the MemberName)
		final Instance memberNameObject = getInstance(state, this.data.operand(0), "MemberName self", FAIL_JBSE, THROW_JAVA_INTERNAL_ERROR, INTERRUPT_SYMBOLIC_VALUE_NOT_ALLOWED_EXCEPTION);
		
		//gets the container class of the member
		final Reference clazzReference = (Reference) memberNameObject.getFieldValue(JAVA_MEMBERNAME_CLAZZ);
		if (state.isNull(clazzReference)) {
			//not resolved
			throwNew(state, this.ctx.getCalculator(), INTERNAL_ERROR);
			exitFromAlgorithm();
		}
		final Instance_JAVA_CLASS clazz = (Instance_JAVA_CLASS) state.getObject(clazzReference);
		final ClassFile containerClass = clazz.representedClass(); 

		//checks if the member is an instance field
		final int fieldFlags = ((Integer) ((Simplex) memberNameObject.getFieldValue(JAVA_MEMBERNAME_FLAGS)).getActualValue()).intValue();
		if ((fieldFlags & IS_FIELD) == 0 || (this.must_be_static ? ((fieldFlags & Modifier.STATIC) == 0) : ((fieldFlags & Modifier.STATIC) != 0))) {
			//not an instance field
			throwNew(state, this.ctx.getCalculator(), INTERNAL_ERROR);
			exitFromAlgorithm();
		}
		
		//gets the offset
		final Signature fieldSignature = new Signature(containerClass.getClassName(), valueString(state, (Reference) memberNameObject.getFieldValue(JAVA_MEMBERNAME_TYPE)), valueString(state, (Reference) memberNameObject.getFieldValue(JAVA_MEMBERNAME_NAME)));
		final int _ofst = containerClass.getFieldOffset(fieldSignature);
		this.ofst = (Simplex) this.ctx.getCalculator().pushInt(_ofst).to(LONG).pop();
	}

	@Override
	protected StrategyUpdate<DecisionAlternative_NONE> updater() {
		return (state, alt) -> {
			state.pushOperand(this.ofst);
		};
	}
}

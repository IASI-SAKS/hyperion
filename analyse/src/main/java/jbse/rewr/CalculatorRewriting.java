package jbse.rewr;

import jbse.common.exc.UnexpectedInternalException;
import jbse.rewr.exc.NoResultException;
import jbse.val.*;
import jbse.val.exc.InvalidOperandException;
import jbse.val.exc.InvalidOperatorException;
import jbse.val.exc.InvalidTypeException;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * A {@link Calculator} based on {@link Rewriter}s.
 * 
 * @author Pietro Braione
 *
 */
public class CalculatorRewriting extends Calculator {
	private final ArrayList<Rewriter> rewriters = new ArrayList<Rewriter>();
	
	/**
	 * Constructor.
	 */
	public CalculatorRewriting() {
		super();
	}
    
    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#add(jbse.mem.Primitive, jbse.mem.Primitive)
	 */
    @Override
	public Primitive add(Primitive firstOperand, Primitive secondOperand) 
	throws InvalidOperandException, InvalidTypeException {
    	try {
    		return applyRewriters(Expression.makeExpressionBinary(this, firstOperand, Operator.ADD, secondOperand));
    	} catch (InvalidOperatorException e) {
    		//this should never happen
    		throw new UnexpectedInternalException(e);
    	}
    }
    
    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#mul(jbse.mem.Primitive, jbse.mem.Primitive)
	 */
    @Override
	public Primitive mul(Primitive firstOperand, Primitive secondOperand)
	throws InvalidOperandException, InvalidTypeException {
    	try {
			return applyRewriters(Expression.makeExpressionBinary(this, firstOperand, Operator.MUL, secondOperand));
		} catch (InvalidOperatorException e) {
    		//this should never happen
    		throw new UnexpectedInternalException(e);
		}
    }
    
    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#sub(jbse.mem.Primitive, jbse.mem.Primitive)
	 */
    @Override
	public Primitive sub(Primitive firstOperand, Primitive secondOperand)
	throws InvalidOperandException, InvalidTypeException {
    	try {
			return applyRewriters(Expression.makeExpressionBinary(this, firstOperand, Operator.SUB, secondOperand));
		} catch (InvalidOperatorException e) {
    		//this should never happen
    		throw new UnexpectedInternalException(e);
		}
    }
    
    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#div(jbse.mem.Primitive, jbse.mem.Primitive)
	 */
    @Override
	public Primitive div(Primitive firstOperand, Primitive secondOperand)
	throws InvalidOperandException, InvalidTypeException {
    	try {
			return applyRewriters(Expression.makeExpressionBinary(this, firstOperand, Operator.DIV, secondOperand));
		} catch (InvalidOperatorException e) {
    		//this should never happen
    		throw new UnexpectedInternalException(e);
		}
    }
    
    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#rem(jbse.mem.Primitive, jbse.mem.Primitive)
	 */
    @Override
	public Primitive rem(Primitive firstOperand, Primitive secondOperand)
	throws InvalidOperandException, InvalidTypeException {
    	try {
			return applyRewriters(Expression.makeExpressionBinary(this, firstOperand, Operator.REM, secondOperand));
		} catch (InvalidOperatorException e) {
    		//this should never happen
    		throw new UnexpectedInternalException(e);
		}
    }
    
    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#neg(jbse.mem.Primitive)
	 */
    @Override
	public Primitive neg(Primitive operand)
	throws InvalidOperandException, InvalidTypeException {
    	try {
			return applyRewriters(Expression.makeExpressionUnary(this, Operator.NEG, operand));
		} catch (InvalidOperatorException e) {
    		//this should never happen
    		throw new UnexpectedInternalException(e);
		}
    }

    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#andBitwise(jbse.mem.Primitive, jbse.mem.Primitive)
	 */
    @Override
	public Primitive andBitwise(Primitive firstOperand, Primitive secondOperand) 
	throws InvalidOperandException, InvalidTypeException {
    	try {
			return applyRewriters(Expression.makeExpressionBinary(this, firstOperand, Operator.ANDBW, secondOperand));
		} catch (InvalidOperatorException e) {
    		//this should never happen
    		throw new UnexpectedInternalException(e);
		}
    }
    
    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#orBitwise(jbse.mem.Primitive, jbse.mem.Primitive)
	 */
    @Override
	public Primitive orBitwise(Primitive firstOperand, Primitive secondOperand) 
	throws InvalidOperandException, InvalidTypeException {
    	try {
			return applyRewriters(Expression.makeExpressionBinary(this, firstOperand, Operator.ORBW, secondOperand));
		} catch (InvalidOperatorException e) {
    		//this should never happen
    		throw new UnexpectedInternalException(e);
		}
    }
    
    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#xorBitwise(jbse.mem.Primitive, jbse.mem.Primitive)
	 */
    @Override
	public Primitive xorBitwise(Primitive first, Primitive param) 
	throws InvalidOperandException, InvalidTypeException {
    	try {
			return applyRewriters(Expression.makeExpressionBinary(this, first, Operator.XORBW, param));
		} catch (InvalidOperatorException e) {
    		//this should never happen
    		throw new UnexpectedInternalException(e);
		}
    }
    
    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#and(jbse.mem.Primitive, jbse.mem.Primitive)
	 */
    @Override
	public Primitive and(Primitive firstOperand, Primitive secondOperand) 
	throws InvalidOperandException, InvalidTypeException {
    	try {
			return applyRewriters(Expression.makeExpressionBinary(this, firstOperand, Operator.AND, secondOperand));
		} catch (InvalidOperatorException e) {
    		//this should never happen
    		throw new UnexpectedInternalException(e);
		}
    }
    
    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#or(jbse.mem.Primitive, jbse.mem.Primitive)
	 */
    @Override
	public Primitive or(Primitive firstOperand, Primitive secondOperand) 
	throws InvalidOperandException, InvalidTypeException {
    	try {
			return applyRewriters(Expression.makeExpressionBinary(this, firstOperand, Operator.OR, secondOperand));
		} catch (InvalidOperatorException e) {
    		//this should never happen
    		throw new UnexpectedInternalException(e);
		}
    }
    
    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#not(jbse.mem.Primitive)
	 */
    @Override
	public Primitive not(Primitive operand)
	throws InvalidOperandException, InvalidTypeException {
    	try {
			return applyRewriters(Expression.makeExpressionUnary(this, Operator.NOT, operand));
		} catch (InvalidOperatorException e) {
    		//this should never happen
    		throw new UnexpectedInternalException(e);
		}
    }
    
    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#shl(jbse.mem.Primitive, jbse.mem.Primitive)
	 */
    @Override
	public Primitive shl(Primitive firstOperand, Primitive secondOperand)
	throws InvalidOperandException, InvalidTypeException {
    	try {
			return applyRewriters(Expression.makeExpressionBinary(this, firstOperand, Operator.SHL, secondOperand));
		} catch (InvalidOperatorException e) {
    		//this should never happen
    		throw new UnexpectedInternalException(e);
		}
    }

    
    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#shr(jbse.mem.Primitive, jbse.mem.Primitive)
	 */
    @Override
	public Primitive shr(Primitive firstOperand, Primitive secondOperand)
	throws InvalidOperandException, InvalidTypeException {
    	try {
			return applyRewriters(Expression.makeExpressionBinary(this, firstOperand, Operator.SHR, secondOperand));
		} catch (InvalidOperatorException e) {
    		//this should never happen
    		throw new UnexpectedInternalException(e);
		}
    }
    
    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#ushr(jbse.mem.Primitive, jbse.mem.Primitive)
	 */
    @Override
	public Primitive ushr(Primitive firstOperand, Primitive secondOperand)
	throws InvalidOperandException, InvalidTypeException {
    	try {
			return applyRewriters(Expression.makeExpressionBinary(this, firstOperand, Operator.USHR, secondOperand));
		} catch (InvalidOperatorException e) {
    		//this should never happen
    		throw new UnexpectedInternalException(e);
		}
    }
    
    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#eq(jbse.mem.Primitive, jbse.mem.Primitive)
	 */
    @Override
	public Primitive eq(Primitive firstOperand, Primitive secondOperand)
	throws InvalidOperandException, InvalidTypeException {
    	try {
			return applyRewriters(Expression.makeExpressionBinary(this, firstOperand, Operator.EQ, secondOperand));
		} catch (InvalidOperatorException e) {
    		//this should never happen
    		throw new UnexpectedInternalException(e);
		}
    }
    
    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#ne(jbse.mem.Primitive, jbse.mem.Primitive)
	 */
    @Override
	public Primitive ne(Primitive firstOperand, Primitive secondOperand)
	throws InvalidOperandException, InvalidTypeException {
    	try {
			return applyRewriters(Expression.makeExpressionBinary(this, firstOperand, Operator.NE, secondOperand));
		} catch (InvalidOperatorException e) {
    		//this should never happen
    		throw new UnexpectedInternalException(e);
		}
    }
    
    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#le(jbse.mem.Primitive, jbse.mem.Primitive)
	 */
    @Override
	public Primitive le(Primitive firstOperand, Primitive secondOperand)
	throws InvalidOperandException, InvalidTypeException {
    	try {
			return applyRewriters(Expression.makeExpressionBinary(this, firstOperand, Operator.LE, secondOperand));
		} catch (InvalidOperatorException e) {
    		//this should never happen
    		throw new UnexpectedInternalException(e);
		}
    }
    
    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#lt(jbse.mem.Primitive, jbse.mem.Primitive)
	 */
    @Override
	public Primitive lt(Primitive firstOperand, Primitive secondOperand)
	throws InvalidOperandException, InvalidTypeException {
    	try {
			return applyRewriters(Expression.makeExpressionBinary(this, firstOperand, Operator.LT, secondOperand));
		} catch (InvalidOperatorException e) {
    		//this should never happen
    		throw new UnexpectedInternalException(e);
		}
    }
    
    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#ge(jbse.mem.Primitive, jbse.mem.Primitive)
	 */
    @Override
	public Primitive ge(Primitive firstOperand, Primitive secondOperand)
	throws InvalidOperandException, InvalidTypeException {
    	try {
			return applyRewriters(Expression.makeExpressionBinary(this, firstOperand, Operator.GE, secondOperand));
		} catch (InvalidOperatorException e) {
    		//this should never happen
    		throw new UnexpectedInternalException(e);
		}
    }
    
    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#gt(jbse.mem.Primitive, jbse.mem.Primitive)
	 */
    @Override
	public Primitive gt(Primitive firstOperand, Primitive secondOperand)
	throws InvalidOperandException, InvalidTypeException {
        try {
			return applyRewriters(Expression.makeExpressionBinary(this, firstOperand, Operator.GT, secondOperand));
		} catch (InvalidOperatorException e) {
    		//this should never happen
    		throw new UnexpectedInternalException(e);
		}
    }

    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#applyFunction(char, java.lang.String, jbse.mem.Primitive)
	 */
    @Override
	public Primitive applyFunction(char type, String operator, Primitive... args) 
	throws InvalidOperandException, InvalidTypeException {
        return applyRewriters(new FunctionApplication(type, this, operator, args));
    }
    
    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#widen(char, jbse.mem.Primitive)
	 */
    @Override
    public Primitive widen(char type, Primitive arg) 
    throws InvalidTypeException, InvalidOperandException {
    	return applyRewriters(WideningConversion.make(type, this, arg));
    }
    
    /* (non-Javadoc)
	 * @see jbse.mem.ICalculator#widen(char, jbse.mem.Primitive)
	 */
    @Override
    public Primitive narrow(char type, Primitive arg) 
    throws InvalidTypeException, InvalidOperandException {
    	return applyRewriters(NarrowingConversion.make(type, this, arg));
    }
    
    /**
     * Adds a rewriter.
     * 
     * @param r the {@link Rewriter} to add.
     */
    public void addRewriter(Rewriter r) {
    	this.rewriters.add(r);
    }
    
    /**
     * Applies a sequence of rewriters to a {@link Primitive}.
     * 
     * @param p a {@link Primitive}.
     * @param rewriters a {@link Rewriter}{@code []}.
     * @return the {@link Primitive} obtained by applying to {@code p} first all 
     *         the {@link Rewriter}s in {@code rewriters}, in their
     *         parameter order, then all the {@link Rewriter}s registered
     *         by subsequent invocations of {@link #addRewriter(Rewriter)}, 
     *         in their invocation order.
     */
    public Primitive applyRewriters(Primitive p, Rewriter...rewriters) {
    	Primitive retVal = p;
    	final ArrayList<Rewriter> toApply = new ArrayList<Rewriter>(Arrays.asList(rewriters));
    	toApply.addAll(this.rewriters);
    	for (Rewriter r : toApply) {
    		try {
    			r.setCalculator(this);
				retVal = r.rewrite(retVal);
			} catch (NoResultException e) {
				//this should not happen
				throw new UnexpectedInternalException(e);
			}
    	}
    	return retVal;
    }
}

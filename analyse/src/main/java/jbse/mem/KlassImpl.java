package jbse.mem;

import jbse.bc.ClassFile;
import jbse.bc.Signature;
import jbse.val.Calculator;
import jbse.val.HistoryPoint;
import jbse.val.KlassPseudoReference;

/**
 * Class that represents the shared portion of an object 
 * in the static method area, i.e., its static fields.
 */
public final class KlassImpl extends ObjektImpl implements Klass {
    private boolean initialized;

    /**
     * Constructor.
     * 
     * @param calc a {@link Calculator}. It must not be {@code null}.
     * @param symbolic a {@code boolean}, whether this object is symbolic
     *        (i.e., not explicitly created during symbolic execution, 
     *        but rather assumed).
     * @param origin a {@link KlassPseudoReference} if this {@link KlassImpl}
     *        exists in the initial state, otherwise {@code null}.
     * @param epoch the creation {@link HistoryPoint} of this {@link KlassImpl}.
     * @param numOfStaticFields an {@code int}, the number of static fields.
     * @param fieldSignatures varargs of field {@link Signature}s, all the
     *        fields this object knows.
     */
    KlassImpl(Calculator calc, boolean symbolic, KlassPseudoReference origin, HistoryPoint epoch, int numOfStaticFields, Signature... fieldSignatures) {
        super(calc, symbolic, null, origin, epoch, true, numOfStaticFields, fieldSignatures);
        this.initialized = false;
    }

	KlassWrapper makeWrapper(StaticMethodArea destinationStaticArea, ClassFile classFile) {
		return new KlassWrapper(destinationStaticArea, classFile, this);
	}

    @Override
    public boolean isInitialized() {
        return this.initialized;
    }

    @Override
    public void setInitialized() {
        this.initialized = true;
    }

    @Override
    public KlassImpl clone() {
        final KlassImpl o = (KlassImpl) super.clone();
        o.fields = fieldsDeepCopy();

        return o;
    }
}

package it.cnr.saks.hyperion.symbolic;

import jbse.dec.exc.DecisionException;

public class GuidanceException extends DecisionException {
    private static final long serialVersionUID = -5041782432117738061L;
    
    public GuidanceException() { super(); }

    public GuidanceException(String s) { super(s); }

    public GuidanceException(Exception e) { super(e); }
}

package it.cnr.iasi.saks.inspection;

public class SimpleInvokes {
    private final String tp;
    private final int seqNum;
    private final int frameEpoch;
    private final String caller;
    private final String callee;
    private final Object[] params;

    public SimpleInvokes(String tp, int seqNum, int frameEpoch, String caller, String callee, Object[] params) {
        this.tp = tp;
        this.seqNum = seqNum;
        this.frameEpoch = frameEpoch;
        this.caller = caller;
        this.callee = callee;
        this.params = params;
    }

    public String getInvokeString() {
        // invokes(TestProgram, BranchingPointList, SeqNum, Caller, ProgramPoint, FrameEpoch, PathCondition, Callee, Parameters).
        StringBuilder sb = new StringBuilder();

        sb.append("invokes(");
        sb.append("'").append(this.tp).append("', ");
        sb.append("[1], ");
        sb.append(this.seqNum).append(", ");
        sb.append("'").append(this.caller.replace('.','/')).append("', ");
        sb.append("1, ");
        sb.append(this.frameEpoch).append(", ");
        sb.append("[], ");
        sb.append("'").append(this.callee.replace('.','/')).append("', ");
        sb.append("[");
        boolean doneFirst = false;
        for(Object p: params) {
            sb.append(doneFirst ? ", " : "");
            doneFirst = true;
            sb.append("'").append(getRepresentation(p)).append("'");
        }
        sb.append("]).");

        return sb.toString();
    }

    private String getRepresentation(Object o) {
        if(o == null)
            return "null";

        if(o instanceof String)
            return (String)o;
        if(o instanceof Integer)
            return ((Integer)o).toString();
        if(o instanceof Double)
            return ((Double)o).toString();
        if(o instanceof Float)
            return ((Float)o).toString();
        if(o instanceof Character)
            return ((Character)o).toString();
        if(o instanceof Boolean)
            return ((Boolean)o).toString();

        return o.getClass().getName();
    }

    public String getTp() {
        return tp;
    }

    public String getCaller() {
        return caller;
    }

    public String getCallee() {
        return callee;
    }

}

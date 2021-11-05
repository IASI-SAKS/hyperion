package it.cnr.saks.hyperion.grouping;

public class GroupingException extends Exception {

    public GroupingException(String s) { super(s); }

    public GroupingException(Exception e) { super(e); }

    private static final long serialVersionUID = -2595679544414588195L;
}
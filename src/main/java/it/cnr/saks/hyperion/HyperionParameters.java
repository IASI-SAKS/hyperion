package it.cnr.saks.hyperion;

import jbse.bc.Signature;
import jbse.dec.DecisionProcedureAlgorithms;
import jbse.jvm.RunnerParameters;
import jbse.rewr.CalculatorRewriting;

import java.util.concurrent.TimeUnit;

public class HyperionParameters implements Cloneable {
    private final RunnerParameters runnerParameters;
    private Signature testProgramSignature;

    public HyperionParameters() {
        this.runnerParameters = new RunnerParameters();
    }

    public RunnerParameters getRunnerParameters() {
        return runnerParameters;
    }

    public Signature getTestProgramSignature() {
        return testProgramSignature;
    }

    public void setTestProgramSignature(Signature testProgramSignature) {
        this.testProgramSignature = testProgramSignature;
    }

    public void setActions(Analyzer.ActionsRun actionsRun) {
        this.runnerParameters.setActions(actionsRun);
    }

    public void setCalculator(CalculatorRewriting calc) {
        this.runnerParameters.setCalculator(calc);
    }

    public void setDecisionProcedure(DecisionProcedureAlgorithms decisionProcedure) {
        this.runnerParameters.setDecisionProcedure(decisionProcedure);
    }

    public void addUserClasspath(String[] pathsArray) {
        this.runnerParameters.addUserClasspath(pathsArray);
    }

    public void setMethodSignature(Signature method) {
        this.runnerParameters.setMethodSignature(method);
    }

    public void setDepthScope(int depthScope) {
        this.runnerParameters.setDepthScope(depthScope);
    }

    public void setTimeout(long time, TimeUnit minutes) {
        this.runnerParameters.setTimeout(time, minutes);
    }

    public void addUninterpreted(String methodClassName, String methodDescriptor, String methodName) {
        this.runnerParameters.addUninterpreted(methodClassName, methodDescriptor, methodName);
    }
}

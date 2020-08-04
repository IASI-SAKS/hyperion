package it.cnr.saks.sisma.testing;

import jbse.algo.exc.CannotManageStateException;
import jbse.bc.exc.InvalidClassFileFactoryClassException;
import jbse.common.exc.ClasspathException;
import jbse.common.exc.InvalidInputException;
import jbse.common.exc.UnexpectedInternalException;
import jbse.dec.*;
import jbse.dec.exc.DecisionException;
import jbse.jvm.Engine;
import jbse.jvm.Runner;
import jbse.jvm.RunnerBuilder;
import jbse.jvm.RunnerParameters;
import jbse.jvm.exc.*;
import jbse.mem.State;
import jbse.mem.exc.ContradictionException;
import jbse.mem.exc.ThreadStackEmptyException;
import jbse.rewr.CalculatorRewriting;
import jbse.rewr.RewriterOperationOnSimplex;
import jbse.rules.ClassInitRulesRepo;
import jbse.rules.LICSRulesRepo;

import java.util.ArrayList;

public final class Analyzer {
    private boolean trackMethods = false;

    private final RunnerParameters runnerParameters;
    private Engine engine;

    private final InformationLogger informationLogger;

    public Analyzer(InformationLogger informationLogger) throws AnalyzerException {
        final CalculatorRewriting calc = createCalculator();

        this.runnerParameters = new RunnerParameters();
        this.runnerParameters.setActions(new ActionsRun());
        this.runnerParameters.setCalculator(calc);
        this.runnerParameters.setDecisionProcedure(createDecisionProcedure(calc));

        this.informationLogger = informationLogger;
    }

    private class ActionsRun extends Runner.Actions {

        @Override
        public boolean atStart() {
            return super.atStart();
        }
        
        @Override
        public boolean atInitial() {
            Analyzer.this.trackMethods = true;
        	return super.atInitial();
        }

        @Override
        public void atEnd() {
            super.atEnd();
        }

        @Override
        public boolean atStepPost() {
            return super.atStepPost();
        }

        @Override
        public boolean atMethodPost() {
            if(Analyzer.this.trackMethods) {
                final State currentState = Analyzer.this.engine.getCurrentState();
                Analyzer.this.informationLogger.onMethodCall(currentState);
            }

            return super.atMethodPost();
        }

    }

    public void run() throws AnalyzerException {
        try {
            final RunnerBuilder rb = new RunnerBuilder();
            final Runner runner = rb.build(this.runnerParameters);
            this.engine = rb.getEngine();
            runner.run();
            this.engine.close();
        } catch (ClasspathException | DecisionException | CannotManageStateException | EngineStuckException | CannotBacktrackException | NonexistingObservedVariablesException | ThreadStackEmptyException | ContradictionException | FailureException | UnexpectedInternalException | CannotBuildEngineException | InitializationException | InvalidClassFileFactoryClassException e) {
            throw new AnalyzerException(e.getMessage());
        }
    }


    private CalculatorRewriting createCalculator() {
        final CalculatorRewriting calc = new CalculatorRewriting();
        calc.addRewriter(new RewriterOperationOnSimplex()); //indispensable
        return calc;
    }


    private DecisionProcedureAlgorithms createDecisionProcedure(CalculatorRewriting calc) throws AnalyzerException {
        //initializes cores
        DecisionProcedure core = new DecisionProcedureAlwSat(calc);

        //wraps cores with external numeric decision procedure
        try {
            final String switchChar = System.getProperty("os.name").toLowerCase().contains("windows") ? "/" : "-";
            final ArrayList<String> z3CommandLine = new ArrayList<>();
            z3CommandLine.add("z3");
            z3CommandLine.add(switchChar + "smt2");
            z3CommandLine.add(switchChar + "in");
            z3CommandLine.add(switchChar + "t:10");
            core = new DecisionProcedureSMTLIB2_AUFNIRA(core, z3CommandLine);
            core = new DecisionProcedureLICS(core, new LICSRulesRepo());
            core = new DecisionProcedureClassInit(core, new ClassInitRulesRepo());

            //sets the result
            return new DecisionProcedureAlgorithms(core);

        } catch (DecisionException | InvalidInputException e) {
            throw new AnalyzerException(e);
        }
    }

    public Analyzer withUserClasspath(String... paths) {
        this.runnerParameters.addUserClasspath(paths);
        return this;
    }

    public Analyzer withMethodSignature(String className, String descriptor, String name) {
        this.runnerParameters.setMethodSignature(className, descriptor, name);
        return this;
    }

    public Analyzer withDepthScope(int depthScope) {
        this.runnerParameters.setDepthScope(depthScope);
        return this;
    }

    public Analyzer withUninterpreted(String methodClassName, String methodDescriptor, String methodName) {
        this.runnerParameters.addUninterpreted(methodClassName, methodDescriptor, methodName);
        return this;
    }

}
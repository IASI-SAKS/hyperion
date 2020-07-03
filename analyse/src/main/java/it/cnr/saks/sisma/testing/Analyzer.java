package it.cnr.saks.sisma.testing;

import jbse.algo.exc.CannotManageStateException;
import jbse.algo.exc.NotYetImplementedException;
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
import jbse.rewr.RewriterCalculatorRewriting;
import jbse.rewr.RewriterOperationOnSimplex;
import jbse.val.PrimitiveSymbolic;
import jbse.val.Simplex;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

public final class Analyzer {
    private boolean registerMethods = false;

    /** The {@link RunParameters} of the symbolic execution. */
    private final AnalyzerParameters parameters;

    /** The {@link Runner} used to run the method. */
    private final Runner runner; //TODO build run object during construction and make this final

    /** The {@link Engine} underlying {@code runner}. */
    private final Engine engine; //TODO build run object during construction and make this final

    /** The {@link DecisionProcedure} used by {@code engine}. */
    private DecisionProcedureAlgorithms decisionProcedure = null; //TODO build run object during construction and make this final

    /** The {@link Timer} for the decision procedure. */
//    private Timer timer = null;

    /** A purely numeric decision procedure for concretization checks. */
    private DecisionProcedureAlgorithms decisionProcedureConcretization = null;

    private final MethodCallSet methodCallSet;

    /**
     * Constructor.
     */
    public Analyzer(AnalyzerParameters parameters) throws CannotBuildEngineException, NonexistingObservedVariablesException, ContradictionException, InitializationException, InvalidClassFileFactoryClassException, NotYetImplementedException, DecisionException, ClasspathException, InvalidInputException {
        this.parameters = parameters;

        this.methodCallSet = parameters.methodCallSet;

        final RunnerParameters runnerParameters = this.parameters.getRunnerParameters();
        runnerParameters.setActions(new ActionsRun());
        final CalculatorRewriting calc = createCalculator();
        runnerParameters.setCalculator(calc);
        createDecisionProcedure(calc);
        runnerParameters.setDecisionProcedure(this.decisionProcedure);
        final RunnerBuilder rb = new RunnerBuilder();
        this.runner = rb.build(this.parameters.getRunnerParameters());
        this.engine = rb.getEngine();
    }

    private class ActionsRun extends Runner.Actions {

        @Override
        public boolean atStart() {
            return super.atStart();
        }
        
        @Override
        public boolean atInitial() {
            Analyzer.this.registerMethods = true;
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
            if(Analyzer.this.registerMethods) {
                final State currentState = Analyzer.this.engine.getCurrentState();
                Analyzer.this.methodCallSet.inspectState(currentState);
            }

            return super.atMethodPost();
        }

    }

    /**
     * Runs the method.
     * 
     * @return an {@code int} value representing an error code, 
     * {@code 0} if everything went ok, {@code 1} if the cause 
     * of the error was external (inputs), {@code 2} if the cause
     * of the error was internal (bugs).
     */
    public void run() {

        // runs
        try {
            this.runner.run();
        } catch (ClasspathException | 
                 DecisionException | CannotManageStateException | 
                 EngineStuckException | CannotBacktrackException | 
                 NonexistingObservedVariablesException e) {
        } catch (ThreadStackEmptyException | ContradictionException |
                 FailureException | UnexpectedInternalException e) {
        }

//        close(); // TODO: reintroduce
    }

    /**
     * Returns the engine's initial state.
     * Convenience for formatter and 
     * decision procedure creation.
     * 
     * @return the initial {@link State}.
     */
    private State getInitialState() {
        return this.engine.getInitialState();
    }

    /**
     * Returns the engine's initial state.
     * Convenience for decision procedure creation.
     * 
     * @return the initial {@link State}.
     */
    private State getCurrentState() {
        return this.engine.getCurrentState();
    }

    /**
     * Returns the decision procedure's current 
     * model or {@code null}. Convenience for
     * formatter creation.
     * 
     * @return a {@link Map}{@code <}{@link PrimitiveSymbolic}{@code ,}{@link Simplex}{@code >}
     *         or {@code null} 
     */
    private Map<PrimitiveSymbolic, Simplex> getModel() {
        try {
            return this.decisionProcedure.getModel();
        } catch (DecisionException e) {
            return null;
        }
    }

    /**
     * Creates a {@link CalculatorRewriting}.
     * 
     * @return the {@link CalculatorRewriting}.
     * @throws CannotBuildEngineException upon failure 
     *         (cannot instantiate rewriters).
     */
    private CalculatorRewriting createCalculator() throws CannotBuildEngineException {
        // try {
        final CalculatorRewriting calc = new CalculatorRewriting();
        calc.addRewriter(new RewriterOperationOnSimplex()); //indispensable
//        for (final Class<? extends RewriterCalculatorRewriting> rewriterClass : this.parameters.getRewriters()) {
//            if (rewriterClass == null) {
//                //no rewriter
//                continue;
//            }
//            final RewriterCalculatorRewriting rewriter = (RewriterCalculatorRewriting) rewriterClass.newInstance();
//            calc.addRewriter(rewriter);
//        }
//        } catch (InstantiationException | IllegalAccessException | UnexpectedInternalException e) {
//            throw new CannotBuildCalculatorException(e);
//        }
        return calc;
    }


    public static enum DecisionProcedureType {
        /** Does not use a decision procedure, all formulas will be considered satisfiable. */
        ALL_SAT,

        /** Uses Z3. */
        Z3,

        /** Uses CVC4. */
        CVC4
    }


    /**
     * Creates the decision procedures in {@code this.decisionProcedure}
     * and {@code this.decisionProcedureConcretization}. 
     * 
     * @param calc a {@link CalculatorRewriting}.
     */
    private void createDecisionProcedure(CalculatorRewriting calc) throws CannotBuildDecisionProcedureException, InvalidInputException {
    //        throws CannotBuildDecisionProcedureException {
//        try {
            final Path path = Paths.get("/usr/bin/z3");//this.parameters.getExternalDecisionProcedurePath();

            //prints some feedback
//            if (this.parameters.getShowInfo()) {
//                if (this.parameters.getDecisionProcedureType() == DecisionProcedureType.Z3) {
//                    log(MSG_TRY_Z3 + (path == null ? "default" : path.toString()) + ".");
//                } else if (this.parameters.getDecisionProcedureType() == DecisionProcedureType.CVC4) {
//                    log(MSG_TRY_CVC4 + (path == null ? "default" : path.toString()) + ".");
//                } else if (this.parameters.getInteractionMode() == InteractionMode.NO_INTERACTION) {
//                    log(MSG_DECISION_BASIC);
//                } else {
//                    log(MSG_DECISION_INTERACTIVE);
//                }
//            }

            //initializes cores
            final boolean needHeapCheck = false;//(this.parameters.getUseConservativeRepOks() || this.parameters.getDoConcretization());
            DecisionProcedure core = new DecisionProcedureAlwSat(calc);
            DecisionProcedure coreNumeric = (needHeapCheck ? new DecisionProcedureAlwSat(calc) : null);

            //wraps cores with external numeric decision procedure
            final DecisionProcedureType type = DecisionProcedureType.Z3;//this.parameters.getDecisionProcedureType();
            try {
                if (type == DecisionProcedureType.ALL_SAT) {
                    //do nothing
                } else if (type == DecisionProcedureType.Z3) {
                    final String switchChar = System.getProperty("os.name").toLowerCase().contains("windows") ? "/" : "-";
                    final ArrayList<String> z3CommandLine = new ArrayList<>();
                    z3CommandLine.add(path == null ? "z3" : path.toString());
                    z3CommandLine.add(switchChar + "smt2");
                    z3CommandLine.add(switchChar + "in");
                    z3CommandLine.add(switchChar + "t:10");
                    core = new DecisionProcedureSMTLIB2_AUFNIRA(core, z3CommandLine);
                    coreNumeric = (needHeapCheck ? new DecisionProcedureSMTLIB2_AUFNIRA(coreNumeric, z3CommandLine) : null);
                } else if (type == DecisionProcedureType.CVC4) {
                    final ArrayList<String> cvc4CommandLine = new ArrayList<>();
                    cvc4CommandLine.add(path == null ? "cvc4" : path.toString());
                    cvc4CommandLine.add("--lang=smt2");
                    cvc4CommandLine.add("--output-lang=smt2");
                    cvc4CommandLine.add("--no-interactive");
                    cvc4CommandLine.add("--incremental");
                    cvc4CommandLine.add("--tlimit-per=10000");
                    core = new DecisionProcedureSMTLIB2_AUFNIRA(core, cvc4CommandLine);
                    coreNumeric = (needHeapCheck ? new DecisionProcedureSMTLIB2_AUFNIRA(coreNumeric, cvc4CommandLine) : null);
                } else {
                    core.close();
                    if (coreNumeric != null) {
                        coreNumeric.close();
                    }
                    throw new CannotBuildDecisionProcedureException("NAY"); // TODO: nonsense message
                }
            } catch (DecisionException | InvalidInputException e) {
                throw new CannotBuildDecisionProcedureException(e);
            }

            //further wraps cores with sign analysis, if required
//            if (this.parameters.getDoSignAnalysis()) {
//                core = new DecisionProcedureSignAnalysis(core);
//                coreNumeric = (needHeapCheck ? new DecisionProcedureSignAnalysis(coreNumeric) : null);
//            }

            //further wraps cores with equality analysis, if required
//            if (this.parameters.getDoEqualityAnalysis()) {
//                core = new DecisionProcedureEquality(core);
//                coreNumeric = (needHeapCheck ? new DecisionProcedureEquality(coreNumeric) : null);
//            }

            //sets the decision procedure for checkers
            if (needHeapCheck) {
                this.decisionProcedureConcretization = new DecisionProcedureAlgorithms(coreNumeric);
            }

            //further wraps core with LICS decision procedure
            if (this.parameters.getUseLICS()) {
                core = new DecisionProcedureLICS(core, this.parameters.getLICSRulesRepo());
            }

            //further wraps core with class init decision procedure
            core = new DecisionProcedureClassInit(core, this.parameters.getClassInitRulesRepo());

            //further wraps core with conservative repOk decision procedure
//            if (this.parameters.getUseConservativeRepOks()) {
//                final RunnerParameters checkerParameters = this.parameters.getConcretizationDriverParameters();
//                checkerParameters.setDecisionProcedure(this.decisionProcedureConcretization);
//                @SuppressWarnings("resource")
//                final DecisionProcedureConservativeRepOk dec =
//                        new DecisionProcedureConservativeRepOk(core, checkerParameters, this.parameters.getConservativeRepOks());
//                dec.setInitialStateSupplier(this::getInitialState);
//                dec.setCurrentStateSupplier(this::getCurrentState);
//                core = dec;
//            }

            //wraps core with custom wrappers
//            for (DecisionProcedureCreationStrategy c : this.parameters.getDecisionProcedureCreationStrategies()) {
//                core = c.createAndWrap(core, calc);
//            }

            //wraps with timer
//            final DecisionProcedureDecoratorTimer tCore = new DecisionProcedureDecoratorTimer(core);
//            this.timer = tCore;
//            core = tCore;

            //wraps with printer if interaction with decision procedure must be shown
//            if (this.parameters.getShowDecisionProcedureInteraction()) {
//                core = new DecisionProcedureDecoratorPrint(core, out);
//            }

            //finally guidance
//            if (this.parameters.isGuided()) {
//                final RunnerParameters guidanceDriverParameters = this.parameters.getGuidanceDriverParameters(calc);
//                if (this.parameters.getShowInfo()) {
//                    log(MSG_TRY_GUIDANCE + guidanceDriverParameters.getMethodSignature() + ".");
//                }
//                try {
//                    if (this.parameters.getGuidanceType() == GuidanceType.JBSE) {
//                        this.guidance = new DecisionProcedureGuidanceJBSE(core, calc, guidanceDriverParameters, this.parameters.getMethodSignature());
//                    } else if (this.parameters.getGuidanceType() == GuidanceType.JDI) {
//                        this.guidance = new DecisionProcedureGuidanceJDI(core, calc, guidanceDriverParameters, this.parameters.getMethodSignature());
//                    } else {
//                        throw new UnexpectedInternalException(ERROR_DECISION_PROCEDURE_GUIDANCE_UNRECOGNIZED + this.parameters.getGuidanceType().toString());
//                    }
//                } catch (GuidanceException | UnexpectedInternalException e) {
//                    err(ERROR_GUIDANCE_FAILED + e.getMessage());
//                    throw new CannotBuildDecisionProcedureException(e);
//                }
//                core = this.guidance;
//            }

            //sets the result
            this.decisionProcedure = ((core instanceof DecisionProcedureAlgorithms) ?
                    (DecisionProcedureAlgorithms) core :
                    new DecisionProcedureAlgorithms(core));
//        } catch (InvalidInputException e) {
//            //this should never happen
//            throw new UnexpectedInternalException(e);
//        }
    }


    private void close() throws DecisionException {
        // quits the numeric decision procedure for the checker
        if (this.decisionProcedureConcretization != null) {
            this.decisionProcedureConcretization.close();
            this.decisionProcedureConcretization = null;
        }

        // quits the engine
        this.engine.close();
    }
}
package it.cnr.saks.sisma.testing;

import jbse.bc.Classpath;
import jbse.bc.Signature;
import jbse.dec.DecisionProcedure;
import jbse.jvm.EngineParameters.BreadthMode;
import jbse.jvm.EngineParameters.StateIdentificationMode;
import jbse.jvm.RunnerParameters;
import jbse.mem.State;
import jbse.rewr.CalculatorRewriting;
import jbse.rewr.RewriterCalculatorRewriting;
import jbse.rules.ClassInitRulesRepo;
import jbse.rules.LICSRulesRepo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class AnalyzerParameters implements Cloneable {

    /**
     * A Strategy for creating {@link DecisionProcedure}s.
     * The strategy receives as inputs the necessary dependencies
     * to inject in it, an must return the decision procedure
     * object.
     *
     * @author Pietro Braione
     *
     */
    @FunctionalInterface
    public interface DecisionProcedureCreationStrategy {
        /**
         * Creates a {@link DecisionProcedure}.
         *
         * @param core a previously built {@link DecisionProcedure}.
         * @param calc a {@link CalculatorRewriting}.
         * @return a new {@link DecisionProcedure} that (possibly) has {@code core}
         *         as Decorator component, or next decision procedure in the
         *         Chain Of Responsibility, and (possibly) uses {@code calc} for
         *         calculations and simplifications.
         */
        DecisionProcedure createAndWrap(DecisionProcedure core, CalculatorRewriting calc);
                //throws CannotBuildDecisionProcedureException;
    }

    public static MethodCallSet methodCallSet;

    /** The runner parameters. */
    private RunnerParameters runnerParameters;

    /** The {@link Class}es of all the rewriters to be applied to terms (order matters). */
    private ArrayList<Class<? extends RewriterCalculatorRewriting>> rewriterClasses = new ArrayList<>();

    /**
     * Whether the engine should use the LICS decision procedure.
     * Set to true by default because the LICS decision procedure
     * also resolves class initialization.
     */
    private boolean useLICS = true;


    private ArrayList<DecisionProcedureCreationStrategy> creationStrategies = new ArrayList<>();

    /** The {@link LICSRuleRepo}, containing all the LICS rules. */
    private LICSRulesRepo repoLICS = new LICSRulesRepo();

    /** The {@link ClassInitRulesRepo}, containing all the class initialization rules. */
    private ClassInitRulesRepo repoInit = new ClassInitRulesRepo();

    /**
     *  Associates classes with the name of their respective
     *  conservative repOK methods.
     */
    private HashMap<String, String> conservativeRepOks = new HashMap<>();

    /** The heap scope for conservative repOK and concretization execution. */
    private HashMap<String, Function<State, Integer>> concretizationHeapScope = new HashMap<>();

    /** The depth scope for conservative repOK and concretization execution. */
    private int concretizationDepthScope = 0;

    /** The count scope for conservative repOK and concretization execution. */
    private int concretizationCountScope = 0;

    /** Should show output on console? */
    private boolean showOnConsole = true;

    /** The name of the output file. */
    private String outFileName = null;

    /**
     * {@code true} iff at the end of a path the engine
     * must check if the path can be concretized.
     */
    private boolean doConcretization = false;

    /**
     *  Associates classes with the name of their respective
     *  concretization methods.
     */
    private HashMap<String, String> concretizationMethods = new HashMap<>();

    /**
     * {@code true} iff the tool info (welcome message,
     * progress of tool initialization, final stats)
     * must be logged.
     */
    private boolean showInfo = true;

    /**
     * {@code true} iff the symbolic execution warnings
     * must be logged.
     */
    private boolean showWarnings = true;

    /**
     * {@code true} iff the interactions between the
     * runner and the decision procedure must be logged to
     * the output.
     */
    private boolean showDecisionProcedureInteraction = false;

    /**
     * The source code path.
     */
    private ArrayList<Path> srcPaths = new ArrayList<>();

    /** Whether the symbolic execution is guided along a concrete one. */
    private boolean guided = false;

    /** The signature of the driver method when guided == true. */
    private Signature driverSignature = null;

    /**
     * Constructor.
     */
    public AnalyzerParameters() {
        this.runnerParameters = new RunnerParameters();
    }

    /**
     * Returns the wrapped {@link RunnerParameters} object.
     * 
     * @return the {@link RunnerParameters} object that backs 
     *         this {@link AnalyzerParameters}.
     */
    public RunnerParameters getRunnerParameters() {
        return this.runnerParameters;
    }

    /**
     * Sets whether the bootstrap classloader should also be used to 
     * load the classes defined by the extensions and application classloaders.
     * This deviates a bit from the Java Virtual Machine Specification, 
     * but ensures in practice a faster loading of classes than by
     * the specification mechanism of invoking the {@link ClassLoader#loadClass(String) ClassLoader.loadClass}
     * method. By default it is set to {@code true}.
     * 
     * @param bypassStandardLoading a {@code boolean}.
     */
    public void setBypassStandardLoading(boolean bypassStandardLoading) {
        this.runnerParameters.setBypassStandardLoading(bypassStandardLoading);
    }
    
    /**
     * Gets whether the bootstrap classloader should also be used to 
     * load the classes defined by the extensions and application classloaders.
     * 
     * @return a {@code boolean}.
     */
    public boolean getBypassStandardLoading() {
        return this.runnerParameters.getBypassStandardLoading();
    }

    /**
     * Gets the Java home.
     * 
     * @return a {@link Path}, the Java home.
     */
    public Path getJavaHome() {
        return this.runnerParameters.getJavaHome();
    }

    /**
     * Adds paths to the user classpath, and cancels the effec
     * 
     * @param paths a varargs of {@link String}s, 
     *        the paths to be added to the user 
     *        classpath.
     * @throws NullPointerException if {@code paths == null}.
     */
    public void addUserClasspath(String... paths) { 
        this.runnerParameters.addUserClasspath(paths);
    }

    /**
     * Builds the classpath.
     * 
     * @return a {@link Classpath} object. 
     * @throws IOException if an I/O error occurs while scanning the classpath.
     */
    public Classpath getClasspath() throws IOException {
        return this.runnerParameters.getClasspath();
    }

    /**
     * Specifies that a method must be treated as an uninterpreted pure
     * function, rather than executed. In the case all the parameters are
     * constant, the method is executed metacircularly.
     * 
     * @param methodClassName the name of the class containing the method.
     * @param methodDescriptor the descriptor of the method.
     * @param methodName the name of the method.
     * @throws NullPointerException if any of the above parameters is {@code null}.
     */
    public void addUninterpreted(String methodClassName, String methodDescriptor, String methodName) {
        this.runnerParameters.addUninterpreted(methodClassName, methodDescriptor, methodName);
    }

    /**
     * Returns the methods that must be treated as
     * uninterpreted pure functions.
     * 
     * @return A {@link List}{@code <}{@link String}{@code []>}, 
     *         where each array is a triple (method class name, 
     *         method parameters, method name).
     */
    public List<String[]> getUninterpreted() {
    	return this.runnerParameters.getUninterpreted();
    }

    /**
     * Sets the signature of the method which must be symbolically executed, 
     * and cancels the effect of any previous call to {@link #setStartingState(State)}.
     * 
     * @param className the name of the class containing the method.
     * @param descriptor the descriptor of the method.
     * @param name the name of the method. 
     * @throws NullPointerException if any of the above parameters is {@code null}.
     */
    public void setMethodSignature(String className, String descriptor, String name) { 
        this.runnerParameters.setMethodSignature(className, descriptor, name); 
    }

    /**
     * Gets the signature of the method which must be symbolically executed.
     * 
     * @return a {@link Signature}, or {@code null} if no method signature
     *         has been provided.
     */
    public Signature getMethodSignature() {
        return this.runnerParameters.getMethodSignature();
    }

    /**
     * Sets a limited depth scope. 
     * The depth of a state is the number of branches above it. If 
     * a state has a depth greater than the depth scope the exploration 
     * of the branch it belongs is interrupted.
     * 
     * @param depthScope an {@code int}, the depth scope.
     */
    public void setDepthScope(int depthScope) { 
        this.runnerParameters.setDepthScope(depthScope); 
    }

    /**
     * Gets whether the engine should use LICS rules
     * to decide on references resolution.
     * 
     * @return {@code true} iff the engine must 
     * use LICS rules.
     */
    public boolean getUseLICS() {
        return this.useLICS;
    }

    /**
     * Returns the {@link LICSRulesRepo} 
     * containing all the LICS rules that
     * must be used.
     * 
     * @return a {@link LICSRulesRepo}. It
     *         is the one that backs this
     *         {@link AnalyzerParameters}, not a
     *         safety copy.
     */
    public LICSRulesRepo getLICSRulesRepo() {
        return this.repoLICS;
    }

    /**
     * Returns the {@link ClassInitRulesRepo} 
     * containing all the class initialization 
     * rules (list of classes that are assumed
     * not to be initialized) that must be used.
     * 
     * @return a {@link ClassInitRulesRepo}. It
     *         is the one that backs this
     *         {@link AnalyzerParameters}, not a
     *         safety copy.
     */
    public ClassInitRulesRepo getClassInitRulesRepo() {
        return this.repoInit;
    }

    /**
     * Gets whether, at the end of each path, it should be
     * checked if the final state can be concretized.
     * 
     * @return a {@code boolean}.
     */
    public boolean getDoConcretization() {
        return this.doConcretization;
    }

    /**
     * Returns the concretization methods of classes.
     * 
     * @return a {@link Map}{@code <}{@link String}{@code , }{@link String}{@code >},
     *         associating a class name to the name of its concretization method.
     */
    public Map<String, String> getConcretizationMethods() {
        return new HashMap<>(this.concretizationMethods);
    }


    /**
     * Returns a new {@link RunnerParameters} that can be used
     * to run a concretization method (sets only scopes).
     * 
     * @return a new instance of {@link RunnerParameters}.
     */
    public RunnerParameters getConcretizationDriverParameters() {
        final RunnerParameters retVal = this.runnerParameters.clone();
        retVal.setStateIdentificationMode(StateIdentificationMode.COMPACT);
        retVal.setBreadthMode(BreadthMode.MORE_THAN_ONE);
        retVal.setHeapScopeComputed(this.concretizationHeapScope);
        retVal.setDepthScope(this.concretizationDepthScope);
        retVal.setCountScope(this.concretizationCountScope);
        retVal.setIdentifierSubregionRoot();
        return retVal;
    }


    @SuppressWarnings("unchecked")
    @Override 
    public AnalyzerParameters clone() {
        final AnalyzerParameters o;
        try {
            o = (AnalyzerParameters) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e); //will not happen
        }
        o.runnerParameters = this.runnerParameters.clone();
        o.rewriterClasses = (ArrayList<Class<? extends RewriterCalculatorRewriting>>) this.rewriterClasses.clone();
        o.repoLICS = this.repoLICS.clone();
        o.repoInit = this.repoInit.clone();
        o.conservativeRepOks = (HashMap<String, String>) this.conservativeRepOks.clone();
        o.concretizationHeapScope = (HashMap<String, Function<State, Integer>>) this.concretizationHeapScope.clone();
        o.creationStrategies = (ArrayList<DecisionProcedureCreationStrategy>) this.creationStrategies.clone();
        o.concretizationMethods = (HashMap<String, String>) this.concretizationMethods.clone();
        o.srcPaths = (ArrayList<Path>) this.srcPaths.clone();
        return o;
    }
}

package it.cnr.saks.hyperion.symbolic;

import com.sun.jdi.Value;
import com.sun.jdi.*;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import jbse.apps.run.ImpureMethodException;
import jbse.bc.ClassFile;
import jbse.bc.Offsets;
import jbse.bc.Signature;
import jbse.common.exc.InvalidInputException;
import jbse.common.exc.UnexpectedInternalException;
import jbse.dec.DecisionProcedure;
import jbse.mem.Frame;
import jbse.mem.State;
import jbse.mem.exc.FrozenStateException;
import jbse.mem.exc.ThreadStackEmptyException;
import jbse.val.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

import static jbse.algo.UtilControlFlow.failExecution;
import static jbse.apps.run.JAVA_MAP_Utils.*;
import static jbse.bc.Signatures.JAVA_MAP_CONTAINSKEY;
import static jbse.common.Type.*;

/**
 * {@link DecisionProcedureGuidance} that uses the installed JVM accessed via JDI to
 * perform concrete execution.
 */
public final class DecisionProcedureGuidanceJDI extends DecisionProcedureGuidance {
	/**
	 * Builds the {@link DecisionProcedureGuidanceJDI}.
	 *
	 * @param component the component {@link DecisionProcedure} it decorates.
	 * @param calc a {@link Calculator}.
	 * @param analyzerParameters the parameters to drive guided execution.
	 * @throws GuidanceException if something fails during creation (and the caller
	 *         is to blame).
	 * @throws InvalidInputException if {@code component == null}.
	 */
	public DecisionProcedureGuidanceJDI(DecisionProcedure component, Calculator calc, AnalyzerParameters analyzerParameters)
			throws GuidanceException, InvalidInputException {
		super(component, new JVMJDI(calc, analyzerParameters));
	}

	@Override
	public void setCurrentStateSupplier(Supplier<State> currentStateSupplier) {
		super.setCurrentStateSupplier(currentStateSupplier);
		((JVMJDI) this.jvm).setCurrentStateSupplier(this.currentStateSupplier);
	}

	private static class JVMJDI extends JVM {
		private static final String ERROR_BAD_PATH = "Failed accessing through a memory access path: ";

		StreamRedirectThread outThread;
		StreamRedirectThread errThread;

		protected VirtualMachine vm;
		private BreakpointRequest breakpoint;
		private  boolean valueDependsOnSymbolicApply;
		private final int numOfFramesAtMethodEntry;
		protected Event currentExecutionPointEvent;
		private final Map<String, ReferenceType> alreadyLoadedClasses = new HashMap<>();

		private final AnalyzerParameters analyzerParameters;

		// Handling of uninterpreted functions
		private final Map<SymbolicApply, SymbolicApplyJVMJDI> symbolicApplyCache = new HashMap<>();
		private final Map<String, List<String>> symbolicApplyOperatorOccurrences = new HashMap<>();
		private String currentHashMapModelMethod;

		private Supplier<State> currentStateSupplier = null;
		private String launchArguments;

		void setCurrentStateSupplier(Supplier<State> currentStateSupplier) {
			this.currentStateSupplier = currentStateSupplier;
		}

		public JVMJDI(Calculator calc, AnalyzerParameters analyzerParameters)
				throws GuidanceException {
			super(calc);
			this.analyzerParameters = analyzerParameters;
			this.vm = createVM();
			goToBreakpoint(this.analyzerParameters.getTestProgramSignature(), 0);
			try {
				this.numOfFramesAtMethodEntry = getCurrentThread().frameCount();
			} catch (IncompatibleThreadStateException e) {
				throw new UnexpectedInternalException(e);
			}
			this.outThread = redirect("Subproc stdout", this.vm.process().getInputStream(), System.out);
			this.errThread = redirect("Subproc stderr", this.vm.process().getErrorStream(), System.err);
		}

		private StreamRedirectThread redirect(String name, InputStream in, OutputStream out) {
			StreamRedirectThread t = new StreamRedirectThread(name, in, out);
			t.setDaemon(true);
			t.start();
			return t;
		}

		/**
		 * StreamRedirectThread is a thread which copies its input to
		 * its output and terminates when it completes.
		 *
		 * @author Robert Field
		 */
		private static class StreamRedirectThread extends Thread {

			private final Reader in;
			private final Writer out;

			private static final int BUFFER_SIZE = 2048;

			/**
			 * Set up for copy.
			 * @param name  Name of the thread
			 * @param in    Stream to copy from
			 * @param out   Stream to copy to
			 */
			StreamRedirectThread(String name, InputStream in, OutputStream out) {
				super(name);
				this.in = new InputStreamReader(in);
				this.out = new OutputStreamWriter(out);
				setPriority(Thread.MAX_PRIORITY - 1);
			}

			/**
			 * Copy.
			 */
			@Override
			public void run() {
				try {
					char[] cbuf = new char[BUFFER_SIZE];
					int count;
					while ((count = this.in.read(cbuf, 0, BUFFER_SIZE)) >= 0) {
						this.out.write(cbuf, 0, count);
					}
					this.out.flush();
				} catch (IOException exc) {
					System.err.println("Child I/O Transfer - " + exc);
				}
			}

			public void flush() {
				try {
					this.out.flush();
				} catch (IOException exc) {
					System.err.println("Child I/O Transfer - " + exc);
				}
			}
		}


		private VirtualMachine createVM()
				throws GuidanceException {
			try {
				final Iterable<Path> classPath = this.analyzerParameters.getRunnerParameters().getClasspath().classPath(); // XXX: userClassPath() ?
				final ArrayList<String> listClassPath = new ArrayList<>();
				classPath.forEach(p -> listClassPath.add(p.toString()));
				final String stringClassPath = String.join(File.pathSeparator, listClassPath.toArray(new String[0]));
				final String mainClass = TestLauncher.class.getName();
				final String testProgramClass = binaryClassName(this.analyzerParameters.getTestProgramSignature().getClassName());
				final String testProgramName = this.analyzerParameters.getTestProgramSignature().getName();
				this.launchArguments = "-classpath \"" + stringClassPath + "\" " + mainClass + " " + testProgramClass + " " + testProgramName;
				return launchTarget(this.launchArguments);
			} catch (IOException e) {
				throw new GuidanceException(e);
			}
		}

		private VirtualMachine launchTarget(String mainArgs) throws GuidanceException {
			final LaunchingConnector connector = findLaunchingConnector();
			final Map<String, Connector.Argument> arguments = connectorArguments(connector, mainArgs);
			try {
				return connector.launch(arguments);
			} catch (IOException | IllegalConnectorArgumentsException | VMStartException exc) {
				throw new GuidanceException(exc);
			}
		}

		private Map<String, Connector.Argument> connectorArguments(LaunchingConnector connector, String mainArgs) {
			final Map<String, Connector.Argument> arguments = connector.defaultArguments();
			final Connector.Argument mainArg = arguments.get("main");
			if (mainArg == null) {
				throw new Error("Bad launching connector");
			}
			mainArg.setValue(mainArgs);
			return arguments;
		}

		protected void goToBreakpoint(Signature sig, int offset) throws GuidanceException {
			//sets event requests
			final EventRequestManager mgr = this.vm.eventRequestManager();
			final ClassPrepareRequest cprr = mgr.createClassPrepareRequest();
			cprr.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
			cprr.enable();

			for (ReferenceType classType: this.vm.allClasses()) {
				this.alreadyLoadedClasses.put(classType.name().replace('.', '/'), classType);
				//System.out.println("ClassLOADED: " + classType.name());
			}

			trySetBreakPoint(sig, offset);

			//executes
			this.vm.resume();

			final EventQueue queue = this.vm.eventQueue();
			boolean stopPointFound = false;
			while (!stopPointFound) {
				try {
					final EventSet eventSet = queue.remove();
					final EventIterator it = eventSet.eventIterator();
					while (!stopPointFound && it.hasNext()) {
						final Event event = it.nextEvent();
						handleClassPrepareEvents(event);
						if (this.breakpoint == null) {
							trySetBreakPoint(sig, offset);
						} else {
							stopPointFound = handleBreakpointEvents(event);
						}
					}
					if (!stopPointFound) {
						eventSet.resume();
					}
				} catch (InterruptedException e) {
					throw new GuidanceException(e);
					//TODO is it ok?
				} catch (VMDisconnectedException e) {
					if (this.errThread != null) {
						this.errThread.flush();
					}
					if (this.outThread != null) {
						this.outThread.flush();
					}
					if (stopPointFound) {
						return; //must not try to disable event requests
					} else {
						throw new GuidanceException("Stop point not found while looking for " + sig + "::" + offset + " : " + e + "\nVM launched as: " + this.launchArguments);
					}
				}
			}
			//disables event requests
			cprr.disable();
			if (this.breakpoint != null) {
				this.breakpoint.disable();
				this.breakpoint = null;
			}
		}

		private void handleClassPrepareEvents(Event event) {
			if (event instanceof ClassPrepareEvent) {
				final ClassPrepareEvent evt = (ClassPrepareEvent) event;
				final ReferenceType classType = evt.referenceType();
				this.alreadyLoadedClasses.put(classType.name().replace('.', '/'), classType);
				for (ReferenceType innerType: classType.nestedTypes()) {
					this.alreadyLoadedClasses.put(innerType.name().replace('.', '/'), innerType);
					//System.out.println("ClassPrepareEvent: Inner-class: " + innerType.name());
				}
				//System.out.println("ClassPrepareEvent: " + classType.name());
			}
		}

		private void trySetBreakPoint(Signature sig, int offset) throws GuidanceException {
			final String stopClassName = sig.getClassName();
			final String stopMethodName = sig.getName();
			final String stopMethodDescr = sig.getDescriptor();
			if (this.alreadyLoadedClasses.containsKey(stopClassName)) {
				final ReferenceType classType = this.alreadyLoadedClasses.get(stopClassName);
				final List<Method> methods = classType.methodsByName(stopMethodName);
				for (Method m: methods) {
					if (stopMethodDescr.equals(m.signature())) {
						//System.out.println("** Set breakpoint at: " + m.locationOfCodeIndex(offset));
						final EventRequestManager mgr = this.vm.eventRequestManager();
						this.breakpoint = mgr.createBreakpointRequest(m.locationOfCodeIndex(offset));
						this.breakpoint.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
						this.breakpoint.enable();
						return;
					}
				}
				throw new GuidanceException("Cannot set breakpoint because there is no method " + stopClassName + "." + stopMethodName + stopMethodDescr);
			}
		}

		protected boolean handleBreakpointEvents(Event event) throws GuidanceException {
			if (this.breakpoint.equals(event.request())) {
				//System.out.println("Breakpoint: stopped at: " + event);
				this.currentExecutionPointEvent = event;
				return true;
			}
			return false;
		}

		private LaunchingConnector findLaunchingConnector() {
			final List<Connector> connectors = Bootstrap.virtualMachineManager().allConnectors();
			for (Connector connector : connectors) {
				if (connector.name().equals("com.sun.jdi.CommandLineLaunch")) {
					return (LaunchingConnector) connector;
				}
			}
			throw new Error("No launching connector");
		}

//		private StackFrame rootFrameConcrete() throws IncompatibleThreadStateException, GuidanceException {
//			final int numFramesFromRoot = numFramesFromRootFrameConcrete();
//			final List<StackFrame> frameStack = getCurrentThread().frames();
//			return frameStack.get(numFramesFromRoot);
//		}
//
//		private StackFrame lastFrameConcrete() throws IncompatibleThreadStateException, GuidanceException {
//			final List<StackFrame> frameStack = getCurrentThread().frames();
//			return frameStack.get(this.numOfFramesAtMethodEntry - 1);
//		}

		private StackFrame findOuterFrameConcrete(SymbolicLocalVariable var) throws IncompatibleThreadStateException, GuidanceException {
			if(!(var instanceof ReferenceSymbolicLocalVariable))
				throw new GuidanceException("Unexpected condition");

			final List<StackFrame> frameStack = getCurrentThread().frames();

			ReferenceSymbolicLocalVariable refVar = (ReferenceSymbolicLocalVariable)var;
			for (StackFrame sf : Reversed.reversed(frameStack)) {
				if(refVar.getGenericSignatureType().equals(sf.location().declaringType().signature()))
					return sf;
			}
			throw new GuidanceException("Unexpected condition");
		}

		protected int numFramesFromRootFrameConcrete() throws IncompatibleThreadStateException, GuidanceException {
			return this.getCurrentThread().frameCount() - this.numOfFramesAtMethodEntry;
		}

		@Override
		public String typeOfObject(ReferenceSymbolic origin) throws GuidanceException, ImpureMethodException {
			final ObjectReference object;
			try {
				object = (ObjectReference) getValue(origin);
			} catch (IndexOutOfBoundsException e) {
				if (!origin.asOriginString().equals(e.getMessage())) {
					System.out.println("[JDI] WARNING: In DecisionProcedureGuidanceJDI.typeOfObject: " + origin.asOriginString() + " leads to invalid throw reference: " + e +
							"\n ** Normally this happens when JBSE wants to extract concrete types for fresh-expands, but the reference is null in the concrete state, thus we can safely assume that no Fresh object shall be considered"
							+ "\n ** However it seems that the considered references do not to match with this assumption in this case.");
				}
				return null; // Origin depends on out-of-bound array access: Fresh expansion is neither possible, nor needed
			}
			if (object == null) {
				return null;
			}
			final StringBuilder buf = new StringBuilder();
			String name = object.referenceType().name();
			boolean isArray = false;
			while (name.endsWith("[]")) {
				isArray = true;
				buf.append("[");
				name = name.substring(0, name.length() - 2);
			}
			buf.append(isPrimitiveOrVoidCanonicalName(name) ? toPrimitiveOrVoidInternalName(name) : (isArray ? REFERENCE : "") + internalClassName(name) + (isArray ? TYPEEND : ""));
			return buf.toString();
		}

		@Override
		public boolean isNull(ReferenceSymbolic origin) throws GuidanceException, ImpureMethodException {
			final ObjectReference object = (ObjectReference) getValue(origin);
			return (object == null);
		}

		@Override
		public boolean areAlias(ReferenceSymbolic first, ReferenceSymbolic second) throws GuidanceException, ImpureMethodException {
			final ObjectReference objectFirst = (ObjectReference) getValue(first);
			final ObjectReference objectSecond = (ObjectReference) getValue(second);
			return ((objectFirst == null && objectSecond == null) ||
					(objectFirst != null && objectFirst.equals(objectSecond)));
		}

		@Override
		public Object getValue(Symbolic origin) throws GuidanceException, ImpureMethodException {
			this.valueDependsOnSymbolicApply = false;
			final Value val = (Value) getJDIValue(origin);
			if (val instanceof IntegerValue) {
				return this.calc.valInt(((IntegerValue) val).intValue());
			} else if (val instanceof BooleanValue) {
				return this.calc.valBoolean(((BooleanValue) val).booleanValue());
			} else if (val instanceof CharValue) {
				return this.calc.valChar(((CharValue) val).charValue());
			} else if (val instanceof ByteValue) {
				return this.calc.valByte(((ByteValue) val).byteValue());
			} else if (val instanceof DoubleValue) {
				return this.calc.valDouble(((DoubleValue) val).doubleValue());
			} else if (val instanceof FloatValue) {
				return this.calc.valFloat(((FloatValue) val).floatValue());
			} else if (val instanceof LongValue) {
				return this.calc.valLong(((LongValue) val).longValue());
			} else if (val instanceof ShortValue) {
				return this.calc.valShort(((ShortValue) val).shortValue());
			} else if (val instanceof ObjectReference) {
				return val;
			} else if (val == null) {
				return null;
			} else {  //val instanceof VoidValue
				throw new GuidanceException("Unexpected JDI VoidValue returned from the concrete evaluation of symbolic value " + origin.asOriginString());
			}
		}

		/**
		 * Returns a JDI object from the concrete state standing
		 * for a {@link Symbolic}.
		 *
		 * @param origin a {@link Symbolic}.
		 * @return either a {@link com.sun.jdi.Value}, or a {@link com.sun.jdi.ReferenceType}, or
		 *         a {@link com.sun.jdi.ObjectReference}.
		 * @throws GuidanceException
		 * @throws ImpureMethodException
		 */
		protected Object getJDIValue(Symbolic origin) throws GuidanceException, ImpureMethodException {
			try {
				if (origin instanceof SymbolicLocalVariable) {
					return getJDIValueLocalVariable((SymbolicLocalVariable) origin);
				} else if (origin instanceof KlassPseudoReference) {
					return getJDIObjectStatic(((KlassPseudoReference) origin).getClassFile().getClassName());
				} else if (origin instanceof SymbolicMemberField) {
					final Object o = getJDIValue(((SymbolicMemberField) origin).getContainer());
					if(o == null)
						return null;
					if (!(o instanceof ReferenceType) && !(o instanceof ObjectReference)) {
						throw new GuidanceException(ERROR_BAD_PATH + origin.asOriginString() + " : Fails because containing object is " + o);
					}
					return getJDIValueField(((SymbolicMemberField) origin), o);
				} else if (origin instanceof PrimitiveSymbolicMemberArrayLength) {
					final Object o = getJDIValue(((PrimitiveSymbolicMemberArrayLength) origin).getContainer() );
					if (!(o instanceof ArrayReference)) {
						throw new GuidanceException(ERROR_BAD_PATH + origin.asOriginString() + " : Fails because containing object is " + o);
					}
					return this.vm.mirrorOf(((ArrayReference) o).length());
				} else if (origin instanceof SymbolicMemberArray) {
					final Object o = getJDIValue(((SymbolicMemberArray) origin).getContainer());
					if (!(o instanceof ArrayReference)) {
						throw new GuidanceException(ERROR_BAD_PATH + origin.asOriginString() + " : Fails because containing object is " + o);
					}
					try {
						final Simplex index = (Simplex) eval(((SymbolicMemberArray) origin).getIndex());
						return ((ArrayReference) o).getValue((Integer) index.getActualValue());
					} catch (ClassCastException e) {
						throw new GuidanceException(e);
					} catch (IndexOutOfBoundsException e) {
						throw new IndexOutOfBoundsException(origin.asOriginString());
					}
				} else if (origin instanceof ReferenceSymbolicMemberMapValue) {
					final ReferenceSymbolicMemberMapValue refSymbolicMemberMapValue = (ReferenceSymbolicMemberMapValue) origin;
					final SymbolicApply javaMapContainsKeySymbolicApply;
					try {
						javaMapContainsKeySymbolicApply = (SymbolicApply) calc.applyFunctionPrimitive(BOOLEAN, refSymbolicMemberMapValue.getHistoryPoint(),
								JAVA_MAP_CONTAINSKEY.toString(), refSymbolicMemberMapValue.getContainer(), refSymbolicMemberMapValue.getKey()).pop();
					} catch (NoSuchElementException | jbse.val.exc.InvalidTypeException | InvalidInputException e) {
						throw new UnexpectedInternalException(e);
					}
					if (!this.symbolicApplyCache.containsKey(javaMapContainsKeySymbolicApply)) {
						throw new GuidanceException(ERROR_BAD_PATH + origin.asOriginString() + " : Fails because cointainsKey was not evaluated before evaluating this GET symbol");
					}
					final SymbolicApplyJVMJDI symbolicApplyVm = this.symbolicApplyCache.get(javaMapContainsKeySymbolicApply);
					if (!(symbolicApplyVm instanceof InitialMapSymbolicApplyJVMJDI)) {
						throw new GuidanceException(ERROR_BAD_PATH + origin.asOriginString() + " : Fails because cointainsKey was evaluated as an ordinary abstractlt-interpreted call, rather than as a JAVA_MAP function");
					}
					final InitialMapSymbolicApplyJVMJDI initialMapSymbolicApplyVm = (InitialMapSymbolicApplyJVMJDI) symbolicApplyVm;
					final Value val = initialMapSymbolicApplyVm.getValueAtKey();
					if (val != null) {
						this.valueDependsOnSymbolicApply = true;
					}
					return val;
				} else if (origin instanceof PrimitiveSymbolicHashCode) {
					if (	this.valueDependsOnSymbolicApply) {
						throw new GuidanceException(ERROR_BAD_PATH + origin.asOriginString() +
								" : Fails because the curret implementation of JDI-guidance does not reliably support"
								+ " decisions that deopend on hashCodes of SymbolicApply symbols or their fields");
					}
					final Object o = getJDIValue(((PrimitiveSymbolicHashCode) origin).getContainer());
					if (!(o instanceof ObjectReference)) {
						throw new GuidanceException(ERROR_BAD_PATH + origin.asOriginString() + " : Fails because containing object is " + o);
					}
					final ObjectReference oRef = (ObjectReference) o;
					return oRef.invokeMethod(getCurrentThread(), oRef.referenceType().methodsByName("hashCode").get(0), Collections.emptyList(), ObjectReference.INVOKE_SINGLE_THREADED);
				} else if (origin instanceof SymbolicApply) {
					//Implicit invariant: when we see a ReferenceSymbolicApply for the first time, JDI is at the call point of the corresponding function
					final SymbolicApply symbolicApply = (SymbolicApply) origin;
					if (!this.symbolicApplyCache.containsKey(symbolicApply)) {
						final SymbolicApplyJVMJDI symbolicApplyVm = startSymbolicApplyVm(symbolicApply);
						this.symbolicApplyCache.put(symbolicApply, symbolicApplyVm);
					}
					final SymbolicApplyJVMJDI symbolicApplyVm = this.symbolicApplyCache.get(symbolicApply);
					this.valueDependsOnSymbolicApply = true;
					return symbolicApplyVm.getRetValue();
				} else {
					throw new GuidanceException(ERROR_BAD_PATH + origin.asOriginString());
				}
			} catch (IncompatibleThreadStateException | AbsentInformationException | InvocationException | InvalidTypeException | ClassNotLoadedException e) {
				throw new GuidanceException(e);
			}
		}

		private SymbolicApplyJVMJDI startSymbolicApplyVm(SymbolicApply symbolicApply) throws GuidanceException, ImpureMethodException {
			/* TODO: Add a strategy to limit the maximum number of SymbolicApplyJVMJDI that we might allocate
			 * to execute uninterpreted functions.
			 * At the moment, we start a new SymbolicApplyJVMJDI for each symbolicApply, and we keep alive all
			 * SymbolicApplyJVMJDIs that handle any symbolicApply of type ReferenceSymbolicApply, because
			 * these might be re-queried at future states for the values of fields within the return object.
			 * However, this can become expensive if there are many invocations of ReferenceSymbolicApply
			 * uninterpreted functions.
			 */
			if (isSymbolicApplyOnInitialMap(this.currentStateSupplier.get().getClassHierarchy(), (jbse.val.Value) symbolicApply)) {
				final String op = this.currentHashMapModelMethod; //the operator is containsKey, but we need to move into the jbse.base.JAVA_MAP method where containskey is being evaluated to obtain the proper value of the key
				final SymbolicMemberField initialMap = (SymbolicMemberField) symbolicApply.getArgs()[0];
				final InitialMapSymbolicApplyJVMJDI symbolicApplyVm = new InitialMapSymbolicApplyJVMJDI(this.calc, this.analyzerParameters, op, initialMap, this.currentStateSupplier);
				symbolicApplyVm.eval_INVOKEX();
				if (symbolicApplyVm.getValueAtKey() == null) {
					// the return value of containsKey is a boolean and there is no Object associated with this key,
					// thus we do not need this vm any further
					symbolicApplyVm.close();
				}
				return symbolicApplyVm;
			}
			final String op = symbolicApply.getOperator();
			String opWithContext = SymbolicApplyJVMJDI.formatContextualSymbolicApplyOperatorOccurrence(op, this.currentStateSupplier.get());
			if (opWithContext == null) {
				throw new GuidanceException("UniterpretedNoContext");
			}
            storeNewSymbolicApplyOperatorContextualOccurrence(op, opWithContext);

			final SymbolicApplyJVMJDI symbolicApplyVm = new SymbolicApplyJVMJDI(this.calc, this.analyzerParameters, op);
			symbolicApplyVm.eval_INVOKEX();

			//If the return value is a primitive, we do not need this vm any further
			if (symbolicApply instanceof PrimitiveSymbolicApply) {
				symbolicApplyVm.close();
			}

			return symbolicApplyVm;
		}

		private void storeNewSymbolicApplyOperatorContextualOccurrence(String symbolicApplyOperator, String symbolicApplyOperatorCallWithContext) {
			if (!this.symbolicApplyOperatorOccurrences.containsKey(symbolicApplyOperator)) {
				this.symbolicApplyOperatorOccurrences.put(symbolicApplyOperator, new ArrayList<>());
			}
			List<String> occurrences = symbolicApplyOperatorOccurrences.get(symbolicApplyOperator);
			occurrences.add(symbolicApplyOperatorCallWithContext);
		}

		private Value getJDIValueLocalVariable(SymbolicLocalVariable var)
				throws GuidanceException, IncompatibleThreadStateException, AbsentInformationException {
			final Value val;
			final String varName = var.getVariableName();
			if ("this".equals(varName)) {
				val = findOuterFrameConcrete(var).thisObject();
//				val = rootFrameConcrete().thisObject();
			} else {
				final LocalVariable variable = findOuterFrameConcrete(var).visibleVariableByName(varName);
//				final LocalVariable variable = rootFrameConcrete().visibleVariableByName(varName);
				if (variable == null) {
					throw new GuidanceException(ERROR_BAD_PATH + "{ROOT}:" + var + ".");
				}
				val = findOuterFrameConcrete(var).getValue(variable);
//				val = rootFrameConcrete().getValue(variable);
			}
			return val;
		}

		private ReferenceType getJDIObjectStatic(String className)
				throws GuidanceException, IncompatibleThreadStateException, AbsentInformationException {
			final List<ReferenceType> classes = this.vm.classesByName(className);
			if (classes.size() == 1) {
				return classes.get(0);
			} else {
				throw new GuidanceException(ERROR_BAD_PATH + "[" + className + "].");
			}
		}

		private Value getJDIValueField(SymbolicMemberField origin, Object o)
		throws GuidanceException {
			if (isInitialMapField(this.currentStateSupplier.get().getClassHierarchy(), (jbse.val.Value) origin)) {
				return cloneInitialMap(getCurrentThread(), o);
			}
			final String fieldName = origin.getFieldName();
			if (o instanceof ReferenceType) {
				//the field is static
				final ReferenceType oReferenceType = ((ReferenceType) o);
				final Field fld = oReferenceType.fieldByName(fieldName);
				if (fld == null) {
					throw new GuidanceException(ERROR_BAD_PATH + origin.asOriginString() + " (missing field " + fieldName + ").");
				}
				try {
					return oReferenceType.getValue(fld);
				} catch (IllegalArgumentException e) {
					throw new GuidanceException(e);
				}
			} else {
				//the field is not static (note that it can be declared in the superclass)
				final ObjectReference oReference = ((ObjectReference) o);
				final String fieldDeclaringClass = binaryClassName(origin.getFieldClass());
				final List<Field> fields = oReference.referenceType().allFields();
				Field fld = null;
				for (Field _fld : fields) {
					if (_fld.declaringType().name().equals(fieldDeclaringClass) && _fld.name().equals(fieldName)) {
						fld = _fld;
						break;
					}
				}
				if (fld == null) {
					throw new GuidanceException(ERROR_BAD_PATH + origin.asOriginString() + " (missing field " + fieldName + ").");
				}
				try {
					return oReference.getValue(fld);
				} catch (IllegalArgumentException e) {
					throw new GuidanceException(e);
				}
			}
		}

		private static Value cloneInitialMap(ThreadReference currentThread, Object o) {
			final ObjectReference initialMapRef = (ObjectReference) o;
			try {
				return initialMapRef.invokeMethod(currentThread, initialMapRef.referenceType().methodsByName("clone").get(0), Collections.emptyList(), ObjectReference.INVOKE_SINGLE_THREADED);
			} catch (InvalidTypeException | ClassNotLoadedException | IncompatibleThreadStateException | InvocationException e) {
				throw new UnexpectedInternalException(e);
			}
		}

		public int getCurrentCodeIndex() throws GuidanceException {
			return (int) getCurrentLocation().codeIndex();
		}

		public ThreadReference getCurrentThread() throws GuidanceException {
			if (this.currentExecutionPointEvent != null) {
				if (this.currentExecutionPointEvent instanceof BreakpointEvent) {
					return ((BreakpointEvent) this.currentExecutionPointEvent).thread();
				} else if (this.currentExecutionPointEvent instanceof StepEvent) {
					return ((StepEvent) this.currentExecutionPointEvent).thread();
				} else if (this.currentExecutionPointEvent instanceof MethodExitEvent) {
					return ((MethodExitEvent) this.currentExecutionPointEvent).thread();
				} else {
					throw new GuidanceException("Unexpected JDI failure: current execution point is neither BreakpointEvent nor StepEvent");
				}
			} else {
				throw new GuidanceException("Unexpected JDI failure: current method entry not known ");
			}
		}

		public Location getCurrentLocation() throws GuidanceException {
			if (this.currentExecutionPointEvent != null) {
				if (this.currentExecutionPointEvent instanceof BreakpointEvent) {
					return ((BreakpointEvent) this.currentExecutionPointEvent).location();
				} else if (this.currentExecutionPointEvent instanceof StepEvent) {
					return ((StepEvent) this.currentExecutionPointEvent).location();
				} else if (this.currentExecutionPointEvent instanceof MethodExitEvent) {
					return ((MethodExitEvent) this.currentExecutionPointEvent).location();
				} else {
					throw new GuidanceException("Unexpected JDI failure: current execution point is neither BreakpointEvent nor StepEvent");
				}
			} else {
				throw new GuidanceException("Unexpected JDI failure: current execution point entry not known ");
			}
		}

		@Override
		public void step(State jbseState) throws GuidanceException {
			// Nothing to do: This version of JVMJDI remains stuck at the initial state of the method under analysis
		}

		private static String jdiMethodClassName(Method jdiMeth) {
			return jdiMeth.toString().substring(0, jdiMeth.toString().indexOf(jdiMeth.name() + '(') - 1).replace('.', '/');
		}

		@Override
		public Signature getCurrentMethodSignature() throws ThreadStackEmptyException {
			try {
				final Method jdiMeth = getCurrentLocation().method();
				final String jdiMethClassName = jdiMethodClassName(jdiMeth);
				final String jdiMethDescr = jdiMeth.signature();
				final String jdiMethName = jdiMeth.name();
				return new Signature(jdiMethClassName, jdiMethDescr, jdiMethName);
			} catch (GuidanceException e) {
				throw new UnexpectedInternalException(e);
			}
		}

		@Override
		public int getCurrentProgramCounter() throws ThreadStackEmptyException {
			try {
				return getCurrentCodeIndex();
			} catch (GuidanceException e) {
				throw new UnexpectedInternalException(e);
			}
		}

		@Override
		protected void close() {
			if (this.vm != null) {
				this.vm.exit(0);

				//obviates to inferior process leak
				this.vm.process().destroyForcibly();
				this.vm = null;
			}
			for (SymbolicApplyJVMJDI symbolicApplyVm: symbolicApplyCache.values()) {
				symbolicApplyVm.close();
			}
		}

	}

	private static class SymbolicApplyJVMJDI extends JVMJDI {
		private final String symbolicApplyOperator;
		public static final String callContextSeparator = "&&";
		private final BreakpointRequest targetMethodExitedBreakpoint;
		protected Value symbolicApplyRetValue;
		private final boolean postInitial;

		public SymbolicApplyJVMJDI(Calculator calc, AnalyzerParameters analyzerParameters, String symbolicApplyOperator)
				throws GuidanceException {
			super(calc, analyzerParameters);
			postInitial = true;
			this.symbolicApplyOperator = symbolicApplyOperator;

			/* We set up a control breakpoint to check if, at any next step, JDI erroneously returns from the method under analysis */
			try {
				final EventRequestManager mgr = this.vm.eventRequestManager();
				final Location callPoint = getCurrentThread().frames().get(1).location(); //the current location in caller frame
				this.targetMethodExitedBreakpoint = mgr.createBreakpointRequest(callPoint.method().locationOfCodeIndex(callPoint.codeIndex() + Offsets.INVOKESPECIALSTATICVIRTUAL_OFFSET));
			} catch (IncompatibleThreadStateException e) {
				throw new UnexpectedInternalException(e);
			}
		}

		// NB: This method return null if the call stack indicate a call nested within hash map models,
		// because these calls do not match actual call in the concrete execution observed with JDI.
		public static String formatContextualSymbolicApplyOperatorOccurrence(String symbolicApplyOperator, State state) {
			StringBuilder callCtxString = new StringBuilder();
			try {
				List<Frame> stack = state.getStack();
				for (int i = 0; i < stack.size(); ++i) {
					final ClassFile methodClass = stack.get(i).getMethodClass();
					if (i != stack.size() - 1 && classImplementsJavaUtilMap(methodClass)) {
						return null; // refuse calls nested within hash map models
					} else {
						callCtxString.append(i > 0 ? SymbolicApplyJVMJDI.callContextSeparator : "").append(stack.get(i).getMethodSignature());
					}

				}
			} catch (FrozenStateException e) {
				//this should never happen
				failExecution(e);
			}
            if (symbolicApplyOperator != null) {
            	callCtxString.append(SymbolicApplyJVMJDI.callContextSeparator).append(symbolicApplyOperator);
            }
            return callCtxString.toString();
		}

		public Value getRetValue() {
			return this.symbolicApplyRetValue;
		}

		@Override
		protected boolean handleBreakpointEvents(Event event) throws GuidanceException {
			if (this.postInitial && this.targetMethodExitedBreakpoint.equals(event.request())) {
				try { //Did we exit from target method? Should not happen
					if (numFramesFromRootFrameConcrete() < 0) {
						throw new UnexpectedInternalException("Exited from target method, while looking for method " + this.symbolicApplyOperator);
					}
				} catch (IncompatibleThreadStateException e) {
					throw new UnexpectedInternalException(e);
				}
			}
			return super.handleBreakpointEvents(event);
		}

		protected void eval_INVOKEX() throws GuidanceException, ImpureMethodException {
			//steps and decides
			stepIntoSymbolicApplyMethod();
			this.symbolicApplyRetValue = stepUpToMethodExit();
		}

		protected void stepIntoSymbolicApplyMethod() throws GuidanceException {
			// Make JDI execute the uninterpreted function that corresponds to the symboliApply
			this.targetMethodExitedBreakpoint.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
			this.targetMethodExitedBreakpoint.enable();
			goToBreakpoint(signatureOf(this.symbolicApplyOperator), 0);
			this.targetMethodExitedBreakpoint.disable();
		}

		private static Signature signatureOf(String unintFuncOperator) {
			final String[] parts = unintFuncOperator.split(":");
			return new Signature(parts[0], parts[1], parts[2]);
		}

		private Value stepUpToMethodExit() throws GuidanceException, ImpureMethodException {
			final int currFrames;
			final Method currMethod;
			try {
				currFrames = getCurrentThread().frameCount();
				currMethod = getCurrentThread().frame(0).location().method();
			} catch (IncompatibleThreadStateException e) {
				throw new GuidanceException(e);
			}

			final EventRequestManager mgr = this.vm.eventRequestManager();
			final MethodExitRequest mexr = mgr.createMethodExitRequest();
			mexr.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
			mexr.enable();
			final ReferenceType classType = this.vm.classesByName("it.cnr.saks.hyperion.symbolic.TestLauncher").get(0);
			final Method method = classType.methodsByName("main").get(0);
			final ArrayList<BreakpointRequest> bkprs = new ArrayList<>();
			try {
				for (Location l : method.allLineLocations()) {
					final BreakpointRequest bkpr = mgr.createBreakpointRequest(l);
					bkpr.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
					bkpr.enable();
					bkprs.add(bkpr);
				}
			} catch (AbsentInformationException e) {
				throw new GuidanceException(e);
			}

			this.vm.resume();
			final EventQueue queue = this.vm.eventQueue();

			MethodExitEvent mthdExitEvent = null;
			boolean exitFound = false;
			eventLoop:
			while (!exitFound) {
				try {
					final EventSet eventSet = queue.remove();
					final EventIterator it = eventSet.eventIterator();
					while (!exitFound && it.hasNext()) {
						final Event event = it.nextEvent();

						if (event instanceof MethodExitEvent) {
							mthdExitEvent = (MethodExitEvent) event;
							final int methodExitFrameCount = mthdExitEvent.thread().frameCount();
							if (methodExitFrameCount < currFrames) {
								//somehow we exited from the method to a caller frame;
								//this may be caused by an exception that is catched,
								//and for which JDI was not able to detect a catch point
								break eventLoop;
							} else if (methodExitFrameCount == currFrames && mthdExitEvent.location().method().equals(currMethod)) {
								exitFound = true;
								this.currentExecutionPointEvent = event;
							}
						} else if (event instanceof BreakpointRequest) {
							//we hit the catch block of the InvocationTargetException
							//of the JDI launcher: the method threw an uncaught exception
							break eventLoop;
						}
					}
					if (!exitFound) {
						eventSet.resume();
					}
				} catch (InterruptedException e) {
					throw new GuidanceException(e);
					//TODO is it ok?
				} catch (VMDisconnectedException | IncompatibleThreadStateException e) {
					throw new GuidanceException(e);
				}
			}

			for (BreakpointRequest bkpr : bkprs) {
				bkpr.disable();
			}
			mexr.disable();
			if (!exitFound) {
				throw new ImpureMethodException();
			}
			return mthdExitEvent.returnValue();
		}
	}

	private static class InitialMapSymbolicApplyJVMJDI extends SymbolicApplyJVMJDI {
		private final ObjectReference initialMapRef;
		private Value valueAtKey;

		public InitialMapSymbolicApplyJVMJDI(Calculator calc, AnalyzerParameters analyzerParameters, String symbolicApplyOperator, SymbolicMemberField initialMapOrigin, Supplier<State> currentStateSupplier)
		throws GuidanceException, ImpureMethodException {
			super(calc, analyzerParameters, symbolicApplyOperator);
			setCurrentStateSupplier(currentStateSupplier);
			this.initialMapRef = (ObjectReference) getJDIValue(initialMapOrigin);
		}

		@Override
		protected void eval_INVOKEX() throws GuidanceException {
			stepIntoSymbolicApplyMethod();
			try {
				final ObjectReference keyRef = (ObjectReference) getCurrentThread().frame(0).getArgumentValues().get(0);
				this.symbolicApplyRetValue = initialMapRef.invokeMethod(getCurrentThread(), initialMapRef.referenceType().methodsByName("containsKey").get(0), Collections.singletonList(keyRef), ObjectReference.INVOKE_SINGLE_THREADED);
				this.valueAtKey = initialMapRef.invokeMethod(getCurrentThread(), initialMapRef.referenceType().methodsByName("get").get(0), Collections.singletonList(keyRef), ObjectReference.INVOKE_SINGLE_THREADED);
			} catch (InvalidTypeException | ClassNotLoadedException | IncompatibleThreadStateException | InvocationException e) {
				throw new GuidanceException("Failed to call method on the concrete HashMap that corresponds to a symbolic HashMap:" + e);
			}
		}

		public Value getValueAtKey() {
			return this.valueAtKey;
		}
	}

	private static class Reversed<T> implements Iterable<T> {
		private final List<T> original;

		public Reversed(List<T> original) {
			this.original = original;
		}

		public @NotNull Iterator<T> iterator() {
			final ListIterator<T> i = original.listIterator(original.size());

			return new Iterator<T>() {
				public boolean hasNext() { return i.hasPrevious(); }
				public T next() { return i.previous(); }
				public void remove() { i.remove(); }
			};
		}

		public static <T> Reversed<T> reversed(List<T> original) {
			return new Reversed<>(original);
		}
	}
}


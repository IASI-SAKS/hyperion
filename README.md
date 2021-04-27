# Hyperion

> Of Hyperion we are told that he was the first to understand, by diligent attention and observation, the movement of both the sun and the moon and the other stars, and the seasons as well, in that they are caused by these bodies, and to make these facts known to others; and that for this reason he was called the father of these bodies, since he had begotten, so to speak, the speculation about them and their nature.    
>  — Diodorus Siculus (5.67.1)

Hyperion is a tool aiming at analysing Java test programs, to generate multiple similarity metrics. To this end, hyperion relies on [JBSE](https://github.com/pietrobraione/jbse) to carry out symbolic execution of JUnit test programs, generate prolog facts, and carry out multiple analyses on these facts.

## Dependencies

There are several dependencies to hyperion:

* JBSE (currently bundled in the project)
* z3
* SWI Prolog

The source is organized as a maven project, so running `mvn build` should be enough to get everything up and running.

`z3` is an external dependency, which should be available in the system path for the tool to correctly run. Similarly,
SWI Prolog must be manually installed in the system.

### Configuring integration with SWI Prolog

To interact with SWI Prolog, hyperion uses JPL. While java bindings are resolved through maven, there is the need to
let JPL know how to interact with SWI Prolog. To this end, some environmental variables should be set. Depending on your
OS, this configuration requires some care. You can refer the official deployment pages, depending on your OS:

* [Linux](https://jpl7.org/DeploymentLinux)
* [Windows](https://jpl7.org/DeploymentWindows)
* [Mac OS](https://jpl7.org/DeploymentMacos)


## Running

Hyperion can be run as:

```bash
java -cp target/hyperion-shaded-1.0-SNAPSHOT.jar it.cnr.saks.hyperion.Main <path to test classes> <path to SUT classes> [additional paths to add in classpath]
```

# Dev Notes

Format for the Prolog facts `invokes` in files like [this one](src/test/resources/inspection-2020-12-03T11:33Z.pl):
```
invokes(TestProgram,        % 1
        BranchingPointList, % 2
        SeqNum,             % 3
        Caller,             % 4
        ProgramPoint,       % 5
        FrameEpoch,         % 6
        PathCondition,      % 7
        Callee,             % 8
        Parameters)         % 9
```

## Playing with Prolog

To load `similarity_relations.pl`:

```prolog
consult('src/main/prolog/similarity_relations.pl').
```

To get a maximal sequence of direct invocations `MSeq` performed by a caller `M` in the test program `TP`:

```prolog
invoke_sequence(TP,M,ISeq), invokes_callees(ISeq,MSeq).
```
(`ISeq` is a list of `invokes`).

## Wrapping @Before and @BeforeEach

To wrap @Before and @BeforeEach methods, we rely on Javassist. In particular, for each test class which is discovered
during the test program analysis, we keep track of every @Before method for each class.
We then dynamically generate a static method in a custom class (TestWrapper) which allocates an object of the test
class, invokes (in random order) all methods annotated as @Before, and then invokes the test program.

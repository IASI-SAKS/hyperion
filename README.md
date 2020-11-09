# Hyperion

> Of Hyperion we are told that he was the first to understand, by diligent attention and observation, the movement of both the sun and the moon and the other stars, and the seasons as well, in that they are caused by these bodies, and to make these facts known to others; and that for this reason he was called the father of these bodies, since he had begotten, so to speak, the speculation about them and their nature.
>  — Diodorus Siculus (5.67.1)

Hyperion is a tool aiming at analysing Java test programs, to generate multiple similarity metrics. To this end, hyperion relies on [JBSE](https://github.com/pietrobraione/jbse) to carry out symbolic execution of JUnit test programs, generate prolog facts, and carry out multiple analyses on these facts.

## Dependencies

There are several dependencies to hyperion:

* JBSE (burrently bundled in the project)
* z3
* ...

The source is organized as a maven project, so running `mvn build` should be enough to get everything up and running.

`z3` is the only external dependency, which should be available in the system path for the tool to correctly run.

## Running

Hyperion can be run as:

`java -cp target/analyse-shaded-1.0-SNAPSHOT.jar it.cnr.saks.hyperion.Main <path to test classes> <path to SUT classes> [additional path to add in classpath]` 


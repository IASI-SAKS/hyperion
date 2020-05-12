# Analyse

Symbolic execution of test programs.

## TL;DR

External dependencies not resolved by maven:

* `javacc`
* `z3`

`z3` path should be placed in `Z3_PATH` in `Main.java`

Paths to jre and to test program are relative to the project root folder.



After running the analysis, the output is stored in `./analyse/JBSE-output.txt`. The current analysis takes time and the output is large.
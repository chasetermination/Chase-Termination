# Overview

This repository contains a tool for checking the termination of the chase procedure when applied for guarded, linear and
simple-linear existential rules. The repository also includes a dataset including 70 owl files from the class EL of Tuple Generating Dependencies(TGDs) for testing and validating the tool's correctness.

The structure of the respository is as follows:

- \"dataset\" includes owl files from the oxford dataset, and programs containing linear and simple-linear set of rules.
- \"JarFiles\" contains the Jar file for the main analyzer.
- \"src\" includes the complete implementation of the termination algorithm for guarded, simple-linear and linear rules.

## Checking Chase Termination for TEL

One of main focuses of this work is on an algorithm called `Linearization`, which converts guarded set of rules to simple-linear set of rules, and based on the acyclicity of the dependency graph for the new set of rules, the algorithm decides whether chase terminates or not. To run the
algorithm for an ontology "ontology.owl", use the following command:

```
cd JarFiles/
java -jar ChaseTermination-TEL.jar [/path/to/owl/file]
```

The tool executes the chase termination algorithm for the class of EL ontologies, runs the `Linearization` algorithm, and returns whether the chase terminates. Additionally, the output
file contains statistics about the program before and after linearization.

This tool requires no input options, and only requires a valid input file with the `.owl` extension to parse.

- terminates-l: true if the chase terminates; false otherwise.
- avg_arity: The average arities of the predicates.
- avg_arity-l: The average arities of the predicates of new set of rules after linearization.
- arity_one_predicates, arity_two_predicates: number of predicates with arity one, and arity two.
- arity_one_predicates-l, arity_two_predicates-l: number of predicates with arity one, and arity two of new set of rules after linearization.
- n_rules, n_predicates, n_exist_vars: The numbers of rules, predicates, and the existential variables in the rules.
- n_rules-l, n_predicates-l, n_exist_vars-l: The numbers of rules, predicates, and the existential variables in the new set of rules after linearization.
- n_nodes-l, n_edges-l, n_special_components-l, n_special_edges-l: The numbers of nodes, edges, and strongly connected components with special edges, and number of special edges in the dependency graph.
- t_graph-l: The time to build the dependency graph.
- t_component-l: The time to find strongly connected components with a special edge and check for their support.
- t_terminates-l: The end-to-end time to check termination.
- t_linear_db-l: The time to linearize the database.
- t_linea_rules-l: The time to linearize the set of rules.

## Checking Chase Termination for Simple-Linear and Linear TGDs
Another main focus of this work is on an algorithm called the `Simplification`. To run this algorithm for a file containing a linear or simple-linear program, use the following command:

```
cd JarFiles/
java -jar ChaseTermination.jar [-f /path/to/rules] [-l] [-ln] [-d /path/to/db/config] [-o /output/file/address]
```
This tool runs the `Simplification` algorithm for linear programs and converts them to a simple-linear program, and then checks for the acyclicity of the program to decide whether chase terminates or not.

The option `-f` is required and specifies the file containing the set of rules.

The option `-l` is optional, and specifies whether the tool should run the termination algorithm for linear rules; if this option is
missing, the tool will run the algorithm for simple-linear rules. However, if this option is missing and the specified set of rules are not simple-linear, the algorithm will not run.
When `-l` is present, the tool requires a database whether the connection information is specified by `-d /path/to/db/config` which
contains db address, username, password, etc, or the facts available in the specified input file with `-f`. However, when `-l` is not given for simple-linear rules, the database can either be based on the facts in the input file, given using `-d [/path/to/db/config]` option, or if the database is missing, the algorithm for simple-linear rules assumes that for each relation, there is at
least one tuple in the database for that relation.

This tool is explicitly designed for linear and simple-linear programs, however, with the `-ln` option, you can remove the non-linear rules from the specified input file, and run the algorithm for the remaining rules.

To specify an output file name, use the option `-o output.res`. The default output file name is `rules.res` if the input
file is `rules.txt`.

The output files contains the following statistics about the input program:

- terminates: true if the chase terminates; false otherwise.
- avg_arity: The average arities of the predicates.
- arity_one_predicates, arity_two_predicates: number of predicates with arity one, and arity two.
- n_rules, n_predicates, n_exist_vars: The numbers of rules, predicates, and the existential variables in the rules.
- n_nodes, n_edges, n_special_components, n_special_edges: The numbers of nodes, edges, strongly connected components with special edges, and number of special edges in the dependency graph
  of the set of rules.
- t_parse: The time to parse the rules in the input file.
- t_graph: The time to build the dependency graph.
- t_comp: The time to find strongly connected components with a special edge and check for their support.
- t_terminate: The end-to-end time to check termination.

For the algorithm for linear rules, which involves dynamic simplification, the tool returns an additional statistics
related to dynamic simplification:

- n_nodes_d, n_edges_d, n_special_edges_d, n_components_d, n_special_components_d: The numbers of nodes, edges, special
  edges, strongly connected components, and strongly connected components with a special edge in the dependency graph of
  the dynamically simplified rules.
- n_facts, n_shapes: The numbers of facts and shapes in the database.
- t_shapes_d, t_shapes_m: The time to find the shapes (in-db and in-memory)
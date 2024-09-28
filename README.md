# Overview

This repository contains a tool for checking the termination of the chase procedure when applied for guarded, linear and
simple-linear existential rules. The repository also includes a dataset including 70 owl files from the class EL of Tuple Generating Dependencies(TGDs) for testing and validating the tool's correctness.

The structure of the respository is as follows:

- \"dataset\" includes owl files from the oxford dataset.
- \"JarFiles\" contains the Jar file for the main analyzer.
- \"src\" includes the complete implementation of the termination algorithm for guarded, simple-linear and linear rules.

## Checking Chase Termination

This work focuses on an algorithm called `Linearization`, which converts guarded set of rules to simple-linear set of rules, and based on the acyclicity of the dependency graph for the new set of rules, the algorithm decides whether chase terminates or not. To run the
algorithm for an ontology "ontology.owl", use the following command:

```
cd JarFiles/
java -jar ChaseTermination-jar-with-dependencies.jar [/path/to/owl/file]
```

The tool executes the chase termination algorithm and returns whether the chase terminates. Additionally, the output
file contains statistics about the program.

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
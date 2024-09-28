package ca.chaseTermination.executer;

import ca.chaseTermination.engine.Program;
import ca.chaseTermination.graph.FCCNode;
import ca.chaseTermination.graph.Node;
import ca.chaseTermination.primitives.Position;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Set;

import static ca.chaseTermination.executer.SyntacticAnalyzer.depthFirstSearch;

public class TerminationAnalyzer {
    private static final Logger log = LogManager.getLogger(TerminationAnalyzer.class);

    public static boolean terminatesSimpleLinear(List<FCCNode> fullyConnectedComponentsGraph, Set<Position> cyclicPositions, Program program) {
        if (cyclicPositions.isEmpty()) {
            log.info("Graph does not have any special cycles and program terminates.");
            return true;
        }

        if (program.database.isEmpty()) {
            log.info("All the cycles are reachable, and the program does not terminate.");
            return false;
        }

        if (fullyConnectedComponentsGraph.stream().anyMatch(node -> (node.special && node.marked))) {
            log.info("a special cycle in graph is marked, and the program will not terminate.");
            return false;
        }

        depthFirstSearch(fullyConnectedComponentsGraph);

        if (fullyConnectedComponentsGraph.stream().anyMatch(node -> (node.special && node.marked))) {
            log.info("a special cycle in graph is marked, and the program will not terminate.");
            return false;
        }

        log.info("No special cycles can be reached with this program, and it terminates successfully.");
        return true;
    }


    public static boolean terminatesLinear(Set<Node> nodes) {
        if (nodes.isEmpty()) {
            log.info("There is no cycles in this linear program, and it terminates.");
            return true;
        }
        log.info("There is a cycle in this linear program, and it will not terminate.");
        return false;
    }

}

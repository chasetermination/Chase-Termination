package ca.chaseTermination.executer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Execute {
    private static final Logger log = LogManager.getLogger(Execute.class);

    public static void main(String[] args) throws Exception {
        String[] files = {
//                        "00360.owl" ,
                                "00574.owl" ,
                                "00399.owl" ,
                                "00372.owl" ,
                                "00377.owl" ,
                                "00495.owl" ,
                                "00494.owl" ,
                                "00396.owl" ,
                                "00392.owl" ,
                                "00373.owl" ,
                                "00402.owl"
        };
        parseFile2(args[0]);
//        String dir = System.getProperty("user.dir") + "/dataset/ontologies/original/ontology/";
//        for (String file : files) {
//            parseFile(dir + file);
//        }
//            String[] newArgs = new String[]{dir + file};
//            for (int i = 0; i < 5; i++) {
//                if (executeClass(newArgs, OntologyManager.class))
//                    continue;
//                    break;
//            }
//            System.out.println("-------------------------------");
//        }
//        System.out.println("HI");

//        if (checkOption(args, "-dg"))
//            executeClass(args, DataGenerator.class);
//        else if (checkOption(args, "-rg"))
//            executeClass(args, ProgramGenerator.class);
//        else if (checkOption(args, "-f"))
//            executeClass(args, OntologyAnalyzer.class);
//        else printHelp();
    }

    public static void printHelp() {
        System.out.println("Usage of chase-termination.jar:\n");

        System.out.println("Data Generation:");
        System.out.println("java -jar chase-termination.jar -dg -d dbconfig -pr n_predicates -tp n_tuples -min min_arity -max max_arity -dm dm_size");
        System.out.println("    -dg: Specifies data generation mode.");
        System.out.println("    -d: The database configuration file.");
        System.out.println("    -pr: The number of predicates.");
        System.out.println("    -tp: The number of tuples.");
        System.out.println("    -min: The minimum arity of predicates.");
        System.out.println("    -max: The maximum arity of predicates.");
        System.out.println("    -dm: The data domain size.");

        System.out.println("Rule Generation:");
        System.out.println("java -jar chase-termination.jar -rg [-d dbconfig] -ru n_rules [-pr n_predicates -min min_arity -max max_arity] [-o rules.txt]");
        System.out.println("    -rg: Specifies rule generation mode.");
        System.out.println("    -ru: The number of rules.");
        System.out.println("    -o: (Optional) Output file and path (the default is rules.txt in the current directory).\n");

        System.out.println("Chase Termination:");
        System.out.println("java -jar chase-termination.jar -f input-file [-l -d dbconfig] [-o output.txt]");
        System.out.println("    -f: The path of the ontology input file for the chase termination process.");
        System.out.println("    -o: (Optional) Ouptut to the specified output file and its path (the default is input-file.res in the same directory of the input file).");
        System.out.println("    -l: (Optional) Checking for linear rules (the default is for simple linear rules).");
        System.out.println("    -d: The database configuration file (used for all operations).\n");
        System.out.println("    -ln: Ignores non-linear rules.\n");

        System.out.println("For any guidance or help, use -h to show this message.");
    }

    private static boolean executeClass(String[] args, Class<?> clazz) {
        int exit = 1;
        ArrayList<String> options = new ArrayList<>(Arrays.asList(args).subList(0, args.length));
        try {
            boolean rc = AnalyzerExec.exec(clazz, options);
            return rc;
        } catch (Exception e) {
            log.error("Errors occurred while running OntologyAnalyzer");
            e.printStackTrace();
            return true;
        }
    }

    private static void parseFile2(String filePath) {
        BufferedReader reader;
        Map<String, List<List<String>>> map = new HashMap<>();

        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line;
            String ontologyName = "";
            System.out.println("Ontology Name,L1,L2,L3,L4,L5,T1,T2,T3,T4,T5");
            while ((line = reader.readLine()) != null) {
                if (line.contains("Reading ontology")) {
                    String temp = line.substring(line.indexOf("Reading ontology...") + 20);
                    if(!map.containsKey(temp)) {
                        if(!ontologyName.isEmpty()) {
                            String out = ontologyName + ",";
                            for (String time : map.get(ontologyName).get(0)) {
                                out += time + ",";
                            }
                            for (String time : map.get(ontologyName).get(1)) {
                                out += time + ",";
                            }
                            out = out.substring(0, out.length() - 1);
                            System.out.println(out);
                        }
                        ontologyName = temp;
                        map.put(ontologyName, new ArrayList<>());
                        map.get(ontologyName).add(new ArrayList<>());
                        map.get(ontologyName).add(new ArrayList<>());
                    }
                }
                else if (line.contains("Linearizing rules")) {
                    line = reader.readLine();
                    String time = line.substring(line.indexOf("[") + 1, line.indexOf("]"));
                    map.get(ontologyName).get(0).add(time);
                }
                else if (line.contains("Total time")) {
                    String time = line.substring(line.indexOf("[") + 1, line.indexOf("]"));
                    map.get(ontologyName).get(1).add(time);
                }
            }
            String out = ontologyName + ",";
            for (String time : map.get(ontologyName).get(0)) {
                out += time + ",";
            }
            for (String time : map.get(ontologyName).get(1)) {
                out += time + ",";
            }
            out = out.substring(0, out.length() - 1);
            System.out.println(out);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void parseFile(String filePath) {
        BufferedReader reader = null;
        String out = filePath.substring(filePath.lastIndexOf("/") + 1) + ",";
        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line;
            int i = 0;
            while (i < 17) {
                line = reader.readLine();
                out += line.substring(line.indexOf(":") + 2) + ",";
                i++;
            }
            out = out.substring(0, out.length() - 1);
            System.out.println(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean checkOption(String[] args, String option) {
        for (String arg : args) {
            if (arg.equals(option)) return true;
        }
        return false;
    }
}

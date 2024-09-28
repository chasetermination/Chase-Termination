package ca.chaseTermination.engine;

import ca.chaseTermination.executer.OntologyAnalyzer;
import ca.chaseTermination.owlapi.ElkReasonerFactory;
import ca.chaseTermination.parser.Parser;
import ca.chaseTermination.primitives.*;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class OntologyManager {
    private static final Logger logger = Logger.getLogger(OntologyManager.class);
    public static OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
    private static final OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
    private static final OWLDataFactory dataFactory = ontologyManager.getOWLDataFactory();
    static Comparator<Shape> toStringComparator = Comparator.comparing(Shape::toString);


    private static Map<String, OWLClass> classMap = new HashMap<>();
    private static Map<String, OWLNamedIndividual> individualMap = new HashMap<>();
    private static Map<String, OWLClassAssertionAxiom> classAssertionAxiomMap = new HashMap<>();


    private static final List<String> ignore = List.of("DbXref", "Definition", "ObsoleteClass", "Subset", "Synonym", "SynonymType");

    public static final String IRI_PREFIX = "http://www.semanticweb.org/owl/owlapi/turtle#";

    public static <T> List<List<T>> getUnarySubsets(List<T> set) {
        List<List<T>> subsets = new ArrayList<>();
        int n = set.size();
        long subsetCount = 1L << n;
        for (int i = 0; i < subsetCount; i++) {
            List<T> subset = new ArrayList<>();

            for (int j = 0; j < n; j++)
                if ((i & (1 << j)) != 0)
                    subset.add(set.get(j));
            subsets.add(subset);
        }
        return subsets;
    }

//    public static <T> List<List<T>> getUnarySubsets(List<T> set) {
//        List<List<T>> subsets = new ArrayList<>();
//        generateSubsets(0, set, new ArrayList<>(), subsets);
//        System.out.println(subsets.size());
//        return subsets;
//    }
//
//    private static <T> void generateSubsets(int index, List<T> set, List<T> current, List<List<T>> subsets) {
//        if (index == set.size()) {
//            subsets.add(new ArrayList<>(current));
//            return;
//        }
//
//        // Exclude the current element and proceed
//        generateSubsets(index + 1, set, current, subsets);
//
//        // Include the current element and proceed
//        current.add(set.get(index));
//        generateSubsets(index + 1, set, current, subsets);
//
//        // Backtrack to remove the last added element
//        current.remove(current.size() - 1);
//    }

    public static void addClassesAndObjectPropertiesToOntology(Program program, OWLOntology newOntology) {
        for (Predicate predicate : program.database.schema.predicates.values()) {
            if (predicate.arity == 1) {
                OWLClass newClass = dataFactory.getOWLClass(IRI.create(IRI_PREFIX + predicate.name));
                OWLDeclarationAxiom declarationAxiom = dataFactory.getOWLDeclarationAxiom(newClass);
                ontologyManager.addAxiom(newOntology, declarationAxiom);
            } else {
                OWLObjectProperty objectProperty = dataFactory.getOWLObjectProperty(IRI.create(IRI_PREFIX + predicate.name));
                OWLDeclarationAxiom declarationAxiom = dataFactory.getOWLDeclarationAxiom(objectProperty);
                ontologyManager.addAxiom(newOntology, declarationAxiom);
            }
        }
    }

    public static void addSubclassAxiomsToOntology(TGD tgd, OWLOntology newOntology) {
        if (tgd.body.getAtoms().get(0).terms.size() == 1
                && tgd.head.getAtoms().get(0).terms.size() == 1
                && tgd.existentialVars.isEmpty()) {
            OWLClass sub = getClassFromOntology(newOntology, tgd.body.getAtoms().get(0).predicate.name);
            OWLClass sup = getClassFromOntology(newOntology, tgd.head.getAtoms().get(0).predicate.name);
            OWLSubClassOfAxiom subclassAxiom = dataFactory.getOWLSubClassOfAxiom(sub, sup);
            ontologyManager.addAxiom(newOntology, subclassAxiom);
        }
    }

    public static void addDomainAndRangeAxiomsToOntology(TGD tgd, OWLOntology newOntology) {

        if (tgd.body.getAtoms().get(0).terms.size() == 2
                && tgd.head.getAtoms().get(0).terms.size() == 1
                && tgd.existentialVars.isEmpty()) {
            OWLObjectProperty property = getObjectPropertyFromOntology(newOntology, tgd.body.getAtoms().get(0).predicate.name);
            OWLClass cls = getClassFromOntology(newOntology, tgd.head.getAtoms().get(0).predicate.name);
            if (tgd.body.getAtoms().get(0).terms.get(0).label
                    .equals(tgd.head.getAtoms().get(0).terms.get(0).label)) {
                OWLObjectPropertyDomainAxiom domainAxiom = dataFactory.getOWLObjectPropertyDomainAxiom(property, cls);
                ontologyManager.addAxiom(newOntology, domainAxiom);
            } else {
                OWLObjectPropertyRangeAxiom rangeAxiom = dataFactory.getOWLObjectPropertyRangeAxiom(property, cls);
                ontologyManager.addAxiom(newOntology, rangeAxiom);
            }
        }
    }

    public static void addRestrictionsToOntology(TGD tgd, OWLOntology newOntology) {
        OWLClass sub = getClassFromOntology(newOntology, tgd.body.getAtoms().get(0).predicate.name);
        OWLClass sup = null;
        OWLObjectProperty property = null;
        for (Atom atom : tgd.head.getAtoms()) {
            if (atom.terms.size() == 1)
                sup = getClassFromOntology(newOntology, atom.predicate.name);
            else
                property = getObjectPropertyFromOntology(newOntology, atom.predicate.name);
        }
        assert property != null;
        assert sup != null;
        OWLObjectSomeValuesFrom restriction = dataFactory.getOWLObjectSomeValuesFrom(property, sup);
        OWLSubClassOfAxiom subclassAxiom = dataFactory.getOWLSubClassOfAxiom(sub, restriction);
        ontologyManager.addAxiom(newOntology, subclassAxiom);
    }

    public static void addAxiomsToOntology(Program program, OWLOntology newOntology) {
        int newConcepts = 1;
        for (TGD tgd : program.tgds) {
            if (tgd.body.getAtoms().size() == tgd.head.getAtoms().size()) {
                addSubclassAxiomsToOntology(tgd, newOntology);
                addDomainAndRangeAxiomsToOntology(tgd, newOntology);
            } else if (tgd.head.getAtoms().size() == 2)
                addRestrictionsToOntology(tgd, newOntology);
            else { //TODO make method
                List<PositiveAtom> atoms = new ArrayList<>(tgd.body.getAtoms());
                List<PositiveAtom> newAtoms = new ArrayList<>();
                for (PositiveAtom atom : atoms) {
                    if (atom.terms.size() == 1 && atom.terms.get(0).label.equals("X")) {
                        newAtoms.add(atom);
                        atoms.remove(atom);
                        break;
                    }
                }
                List<PositiveAtom> concepts = atoms.stream().filter(a -> a.terms.size() == 1).toList();
                List<PositiveAtom> roles = atoms.stream().filter(a -> a.terms.size() == 2).toList();
                for (PositiveAtom concept : concepts) {
                    String x = concept.toString();
                    x = x.substring(x.indexOf('(') + 1, x.indexOf(')'));
                    for (PositiveAtom role : roles) {
                        String y = role.toString();
                        y = y.substring(y.indexOf('(') + 1, y.indexOf(')'));
                        if (Arrays.stream(y.split(",")).toList().contains(x)) {
                            OWLClass sub = getClassFromOntology(newOntology, concept.predicate.name);
                            OWLClass newClass = dataFactory.getOWLClass(IRI.create(IRI_PREFIX + "Z" + newConcepts));
                            OWLDeclarationAxiom declarationAxiom = dataFactory.getOWLDeclarationAxiom(newClass);
                            ontologyManager.addAxiom(newOntology, declarationAxiom);
                            OWLObjectProperty property = getObjectPropertyFromOntology(newOntology, role.predicate.name);
                            OWLObjectSomeValuesFrom restriction = dataFactory.getOWLObjectSomeValuesFrom(property, sub);
                            OWLEquivalentClassesAxiom equivalentClassesAxiom = dataFactory.getOWLEquivalentClassesAxiom(newClass, restriction);
                            ontologyManager.addAxiom(newOntology, equivalentClassesAxiom);
                            newAtoms.add(new PositiveAtom(new Predicate("Z" + newConcepts, 1), List.of(new Variable("X"))));
                            newConcepts++;
                        }
                    }
                }
                while (newAtoms.size() > 2) {
                    PositiveAtom atom1 = newAtoms.get(1);
                    PositiveAtom atom2 = newAtoms.get(2);
                    newAtoms.remove(atom1);
                    newAtoms.remove(atom2);
                    OWLClass cls1 = dataFactory.getOWLClass(IRI.create(IRI_PREFIX + atom1.predicate.name));
                    OWLClass cls2 = dataFactory.getOWLClass(IRI.create(IRI_PREFIX + atom2.predicate.name));
                    OWLObjectIntersectionOf intersection = dataFactory.getOWLObjectIntersectionOf(cls1, cls2);
                    OWLClass intersectionClass = dataFactory.getOWLClass(IRI.create(IRI_PREFIX + "Z" + newConcepts));
                    OWLDeclarationAxiom declarationAxiom = dataFactory.getOWLDeclarationAxiom(intersectionClass);
                    ontologyManager.addAxiom(newOntology, declarationAxiom);
                    OWLEquivalentClassesAxiom equivalentClassesAxiom = dataFactory.getOWLEquivalentClassesAxiom(intersectionClass, intersection);
                    ontologyManager.addAxiom(newOntology, equivalentClassesAxiom);
                    newAtoms.add(new PositiveAtom(new Predicate("Z" + newConcepts, 1), List.of(new Variable("X"))));
                    newConcepts++;
                }
                PositiveAtom atom1 = newAtoms.get(0);
                PositiveAtom atom2 = newAtoms.get(1);
                OWLClass cls1 = dataFactory.getOWLClass(IRI.create(IRI_PREFIX + atom1.predicate.name));
                OWLClass cls2 = dataFactory.getOWLClass(IRI.create(IRI_PREFIX + atom2.predicate.name));
                OWLObjectIntersectionOf intersection = dataFactory.getOWLObjectIntersectionOf(cls1, cls2);
                OWLClass intersectionClass = dataFactory.getOWLClass(IRI.create(IRI_PREFIX + "Z" + newConcepts));
                OWLDeclarationAxiom declarationAxiom = dataFactory.getOWLDeclarationAxiom(intersectionClass);
                ontologyManager.addAxiom(newOntology, declarationAxiom);
                OWLEquivalentClassesAxiom equivalentClassesAxiom = dataFactory.getOWLEquivalentClassesAxiom(intersectionClass, intersection);
                ontologyManager.addAxiom(newOntology, equivalentClassesAxiom);
                newAtoms.add(new PositiveAtom(new Predicate("Z" + newConcepts, 1), List.of(new Variable("X"))));
                newConcepts++;
                OWLClass sup = getClassFromOntology(newOntology, tgd.head.getAtoms().get(0).predicate.name);
                OWLSubClassOfAxiom subclassAxiom = dataFactory.getOWLSubClassOfAxiom(intersectionClass, sup);
                ontologyManager.addAxiom(newOntology, subclassAxiom);
            }
        }
    }

    public static OWLOntology generateOntology(Program program) throws OWLOntologyCreationException {
        OWLOntology newOntology = ontologyManager.createOntology();
        addClassesAndObjectPropertiesToOntology(program, newOntology);
        addIndividualsToOntology((InMemoryDatabase) program.database, newOntology);
        addAxiomsToOntology(program, newOntology);
        return newOntology;
    }

    public static OWLNamedIndividual getIndividualsFromOntology(OWLReasoner reasoner, String name) {
        Optional<OWLNamedIndividual> individual = reasoner.getRootOntology().getIndividualsInSignature().stream()
                .filter(ind -> ind.getIRI().getShortForm().equals(name))
                .findFirst();
        return individual.orElse(null);
    }

    public static Program generateProgram(OWLOntology ontology) {
        Program program = new Program();
        program.database = new InMemoryDatabase();
        program.database.program = program;
        addPredicatesToProgram(ontology, program);
        addRulesToProgram(ontology, program);
        return program;
    }

    public static void addPredicatesToProgram(OWLOntology ontology, Program program) {
        addClassesAsPredicatesToProgram(ontology, program);
        addObjectPropertiesAsPredicatesToProgram(ontology, program);
    }

    public static void addRulesToProgram(OWLOntology ontology, Program program) {
        addSubClassRelationsAsTGDsToProgram(ontology, program);
        addEquivalentClassRelationsAsTGDsToProgram(ontology, program);
        addPropertyDomainAndRangeAsTGDsToProgram(ontology, program);
        addRestrictionsAsTGDsToProgram(ontology, program);
        addIntersectionsAsTGDsToProgram(ontology, program);
    }

    public static void addIntersectionsAsTGDsToProgram(OWLOntology ontology, Program program) {
        for (OWLEquivalentClassesAxiom axiom : ontology.getAxioms(AxiomType.EQUIVALENT_CLASSES)) {
            OWLClassExpression cls1 = axiom.getClassExpressionsAsList().get(0);
            OWLClassExpression cls2 = axiom.getClassExpressionsAsList().get(1);
            if (cls2 instanceof OWLObjectIntersectionOf intersectionOf) {
                String tgd1 = cls1.asOWLClass().getIRI().getShortForm() + "(X) :- ";
                for (OWLClassExpression cls : intersectionOf.getOperands()) {
                    tgd1 += cls.asOWLClass().getIRI().getShortForm() + "(X),";
                    String rule = cls.asOWLClass().getIRI().getShortForm() + "(X) :- " + cls1.asOWLClass().getIRI().getShortForm() + "(X).";
                    Parser.addRulesOrFactsToProgram(program, rule);
                }
                tgd1 = tgd1.substring(0, tgd1.lastIndexOf(',')) + ".";
                Parser.addRulesOrFactsToProgram(program, tgd1);
            }
        }
    }

    public static void addSubclassRestrictionsAsTGDsToProgram(OWLOntology ontology, Set<String> TGDs) {
        for (OWLSubClassOfAxiom subClassAxiom : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
            OWLClassExpression subClass = subClassAxiom.getSubClass();
            OWLClassExpression superClass = subClassAxiom.getSuperClass();
            if (superClass instanceof OWLObjectSomeValuesFrom someValuesFrom) {
                OWLObjectPropertyExpression property = someValuesFrom.getProperty();
                OWLClassExpression filler = someValuesFrom.getFiller();
                if (!property.isAnonymous() && !filler.isAnonymous() && !subClass.isAnonymous()) {
                    String tgd1 = String.format("%s(X,Y) :- %s(X).",
                            property.asOWLObjectProperty().getIRI().getShortForm(),
                            subClass.asOWLClass().getIRI().getShortForm());
                    String tgd2 = String.format("%s(Y) :- %s(X,Y),%s(X).",
                            filler.asOWLClass().getIRI().getShortForm(),
                            property.asOWLObjectProperty().getIRI().getShortForm(),
                            subClass.asOWLClass().getIRI().getShortForm());
                    TGDs.add(tgd1);
                    TGDs.add(tgd2);
                }
            }
        }
    }

    public static void addEquivalentClassesRestrictionsAsTGDsToProgram(OWLOntology ontology, Set<String> TGDs) {
        for (OWLEquivalentClassesAxiom equivalentClassesAxiom : ontology.getAxioms(AxiomType.EQUIVALENT_CLASSES)) {
            OWLClassExpression subClass = equivalentClassesAxiom.getClassExpressionsAsList().get(0);
            OWLClassExpression superClass = equivalentClassesAxiom.getClassExpressionsAsList().get(1);
            if (superClass instanceof OWLObjectSomeValuesFrom someValuesFrom) {
                OWLObjectPropertyExpression property = someValuesFrom.getProperty();
                OWLClassExpression filler = someValuesFrom.getFiller();
                if (!property.isAnonymous() && !filler.isAnonymous() && !subClass.isAnonymous()) {
                    String tgd1 = String.format("%s(X,Y) :- %s(X).",
                            property.asOWLObjectProperty().getIRI().getShortForm(),
                            subClass.asOWLClass().getIRI().getShortForm());
                    String tgd2 = String.format("%s(Y) :- %s(X,Y),%s(X).",
                            filler.asOWLClass().getIRI().getShortForm(),
                            property.asOWLObjectProperty().getIRI().getShortForm(),
                            subClass.asOWLClass().getIRI().getShortForm());
                    String tgd3 = String.format("%s(X) :- %s(X,Y),%s(Y).",
                            subClass.asOWLClass().getIRI().getShortForm(),
                            property.asOWLObjectProperty().getIRI().getShortForm(),
                            filler.asOWLClass().getIRI().getShortForm());
                    TGDs.add(tgd1);
                    TGDs.add(tgd2);
                    TGDs.add(tgd3);
                }
            }
        }
    }

    public static void addRestrictionsAsTGDsToProgram(OWLOntology ontology, Program program) {
        Set<String> TGDs = new HashSet<>();
        addSubclassRestrictionsAsTGDsToProgram(ontology, TGDs);
        addEquivalentClassesRestrictionsAsTGDsToProgram(ontology, TGDs);
        TGDs.forEach(tgd -> Parser.addRulesOrFactsToProgram(program, tgd));
    }

    public static void addPropertyDomainAndRangeAsTGDsToProgram(OWLOntology ontology, Program program) {
        Set<String> TGDs = new HashSet<>();
        for (OWLObjectProperty property : ontology.getObjectPropertiesInSignature()) {
            addDomainAsTGDToProgram(ontology, property, TGDs);
            addRangeAsTGDToProgram(ontology, property, TGDs);
        }
        TGDs.forEach(tgd -> Parser.addRulesOrFactsToProgram(program, tgd));
    }

    private static void addRangeAsTGDToProgram(OWLOntology ontology, OWLObjectProperty property, Set<String> TGDs) {
        Set<OWLObjectPropertyRangeAxiom> rangeAxioms = ontology.getObjectPropertyRangeAxioms(property);
        for (OWLObjectPropertyRangeAxiom rangeAxiom : rangeAxioms) {
            OWLClassExpression range = rangeAxiom.getRange();
            if (!range.isAnonymous()) {
                String rangeClassName = range.asOWLClass().getIRI().getShortForm();
                TGDs.add(rangeClassName + "(Y) :- " + property.getIRI().getShortForm() + "(X,Y)");
            }
        }
    }

    private static void addDomainAsTGDToProgram(OWLOntology ontology, OWLObjectProperty property, Set<String> TGDs) {
        Set<OWLObjectPropertyDomainAxiom> domainAxioms = ontology.getObjectPropertyDomainAxioms(property);
        for (OWLObjectPropertyDomainAxiom domainAxiom : domainAxioms) {
            OWLClassExpression domain = domainAxiom.getDomain();
            if (!domain.isAnonymous()) {
                String domainClassName = domain.asOWLClass().getIRI().getShortForm();
                TGDs.add(domainClassName + "(X) :- " + property.getIRI().getShortForm() + "(X,Y)");
            }
        }
    }

    public static void addObjectPropertyIndividualsAsFactsToProgram(OWLOntology ontology, Program program) {
        for (OWLObjectPropertyAssertionAxiom axiom : ontology.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
            OWLIndividual subject = axiom.getSubject();
            OWLIndividual object = axiom.getObject();
            OWLObjectPropertyExpression property = axiom.getProperty();

            if (!property.isAnonymous() && subject instanceof OWLNamedIndividual && object instanceof OWLNamedIndividual) {
                String subjectName = ((OWLNamedIndividual) subject).getIRI().getShortForm();
                String objectName = ((OWLNamedIndividual) object).getIRI().getShortForm();
                String propertyName = property.asOWLObjectProperty().getIRI().getShortForm();

                Constant term1 = new Constant(subjectName);
                Constant term2 = new Constant(objectName);
                Predicate predicate = program.database.schema.predicates.get(propertyName);
                Fact fact = new Fact(predicate, List.of(term1, term2));
                ((InMemoryDatabase) program.database).addFact(fact);
            }
        }

    }

    public static void addEquivalentClassRelationsAsTGDsToProgram(OWLOntology ontology, Program program) {
        for (OWLEquivalentClassesAxiom axiom : ontology.getAxioms(AxiomType.EQUIVALENT_CLASSES)) {
            if (axiom.getClassExpressions().size() == 2) {
                Iterator<OWLClassExpression> iterator = axiom.getClassExpressions().iterator();
                OWLClassExpression firstExpr = iterator.next();
                OWLClassExpression secondExpr = iterator.next();

                if (!firstExpr.isAnonymous() && !secondExpr.isAnonymous()) {
                    OWLClass firstClass = firstExpr.asOWLClass();
                    OWLClass secondClass = secondExpr.asOWLClass();

                    String firstDirection = firstClass.getIRI().getShortForm() + "(X)" + " :- " + secondClass.getIRI().getShortForm() + "(X)";
                    String secondDirection = secondClass.getIRI().getShortForm() + "(X)" + " :- " + firstClass.getIRI().getShortForm() + "(X)";
                    Parser.addRulesOrFactsToProgram(program, firstDirection);
                    Parser.addRulesOrFactsToProgram(program, secondDirection);
                }
            }
        }
    }

    public static void addSubClassRelationsAsTGDsToProgram(OWLOntology ontology, Program program) {
        for (OWLSubClassOfAxiom axiom : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
            OWLClassExpression subClass = axiom.getSubClass();
            OWLClassExpression superClass = axiom.getSuperClass();

            if (!subClass.isAnonymous() && !superClass.isAnonymous()) {
                OWLClass subCls = subClass.asOWLClass();
                OWLClass superCls = superClass.asOWLClass();
                String stringTGD = superCls.getIRI().getShortForm() + "(X)" + " :- " + subCls.getIRI().getShortForm() + "(X)";
                Parser.addRulesOrFactsToProgram(program, stringTGD);
            }
        }
    }

    public static void addClassesAsPredicatesToProgram(OWLOntology ontology, Program program) {
        for (OWLClass cls : ontology.getClassesInSignature()) {
            String predicateName = cls.getIRI().getShortForm();
            Predicate predicate = new Predicate(predicateName, 1, program);
            program.database.schema.addPredicate(predicate);
        }
    }

    public static void addObjectPropertiesAsPredicatesToProgram(OWLOntology ontology, Program program) {
        for (OWLObjectProperty objectProperty : ontology.getObjectPropertiesInSignature()) {
            String predicateName = objectProperty.getIRI().getShortForm();
            Predicate predicate = new Predicate(predicateName, 2, program);
            program.database.schema.addPredicate(predicate);
        }
    }

    public static InMemoryDatabase linearize(InMemoryDatabase database, OWLReasoner reasoner) {
        InMemoryDatabase linearizedDatabase = new InMemoryDatabase();
        for (Fact fact : database.getFacts()) {
            if (ignore.contains(fact.predicate.name))
                continue;
            Type type = database.findType(fact, reasoner);
            Predicate lPred = new Predicate(type);
            ArrayList<Term> terms = new ArrayList<>();
//            Fact lFact = new Fact(lPred, terms);
//            terms.add(new Constant("a"));
//            if (lPred.arity == 2) {
//                if (fact.terms.get(0).equals(fact.terms.get(1)))
//                    terms.add(new Constant("a"));
//                else
//                    terms.add(new Constant("b"));
//            }
            Fact lFact = new Fact(lPred, new ArrayList<>(new HashSet(fact.terms)));
            linearizedDatabase.addFact(lFact);
        }
        return linearizedDatabase;
    }

    public static Program linearize(Program program, InMemoryDatabase linearizedDB, OWLReasoner reasoner) {
        Program result = new Program();
        Map<String, Set<Fact>> delta = createFactIndex(linearizedDB.getFacts());
        Set<String> visited = new HashSet<>();
        Map<String, Set<TGD>> tgdIndex = createRulesIndex(program.tgds);
        while (!delta.isEmpty()) {
            int size = 0;
            for (String pname : delta.keySet()) {
                size += delta.get(pname).size();
                for (Fact fact : delta.get(pname)) {
                    visited.add(fact.toString());
                    if (fact.predicate.arity == 2) {
                        Fact f = new Fact(fact.predicate, new ArrayList<>(fact.terms));
                        Collections.reverse(f.terms);
                        visited.add(f.toString());
                    }
                }
            }
            logger.debug("delta content:" + size + " " + " visited " + visited.size() + " result tgds " + result.tgds.size());
            Map<String, Set<Fact>> newFacts = new HashMap<>();
            for (String pname : tgdIndex.keySet()) {
                if (delta.keySet().contains(pname)) {
                    for (TGD tgd : tgdIndex.get(pname)) {
                        linearizeRule(tgd, reasoner, delta.get(pname), result, newFacts, visited);
                    }
                }
            }
            delta.clear();
            delta = newFacts;
        }
        return result;
    }

    private static Map<String, Set<TGD>> createRulesIndex(Set<TGD> tgds) {
        HashMap<String, Set<TGD>> tgdMap = new HashMap<>();
        for (TGD tgd : tgds) {
            List<Atom> atoms = getGuardAndSides(tgd);
            String pname = atoms.get(0).predicate.name;
            if (!tgdMap.containsKey(pname)) tgdMap.put(pname, new HashSet<>());
            tgdMap.get(pname).add(tgd);
        }
        return tgdMap;
    }

    private static Map<String, Set<Fact>> createFactIndex(Fact[] facts) {
        HashMap<String, Set<Fact>> pFactMap = new HashMap<>();
        for (Fact fact : facts) {
            String name = fact.predicate.type.guard.predicate.name;
            if (!pFactMap.containsKey(name)) pFactMap.put(name, new HashSet<>());
            pFactMap.get(name).add(fact);
        }
        return pFactMap;
    }

    private static boolean shapesMatch(Shape fGShape, Shape tGShape) {
        if (fGShape.annotation.equals(tGShape.annotation))
            return true;
        return tGShape.annotation.equals("12") && fGShape.annotation.equals("11");
    }

    private static void linearizeRule(TGD tgd, OWLReasoner reasoner, Set<Fact> facts, Program result, Map<String, Set<Fact>> newFacts, Set<String> visited) {
//        logger.debug("linearize [" + tgd + "] with " + facts.size() + " facts...");
        List<Atom> atoms = getGuardAndSides(tgd);
        Atom tGuard = atoms.get(0);
        Atom tSide = null;
        if (atoms.size() > 1) tSide = atoms.get(1);
        Shape tGShape = getShape(tGuard);
        for (Fact fact : facts) {
            Shape fGShape = fact.predicate.type.guard;
            if (!shapesMatch(fGShape, tGShape))
                continue;
            boolean shapeFound = false;
            if (tSide != null) {
                for (Shape fSide : fact.predicate.type.shapes) {
                    if (fSide.predicate.equals(tSide.predicate)) {
                        shapeFound = true;
                        break;
                    }
                }
                if (!shapeFound)
                    continue;
            }
            Map<Variable, Term> vMap = new HashMap<>();
            int i = 0;
            for (Term term : tGuard.terms) {
                Variable v = (Variable) term;
                vMap.put(v, fact.terms.get(i));
                if (fGShape.annotation.equals("11"))
                    continue;
                i++;
            }
            for (Variable v : tgd.existentialVars) {
                if (!vMap.containsKey(v)) vMap.put(v, new Null());
            }
            TGD lTGD = new TGD();
            lTGD.body = new Conjunct();
            Set<Term> bTerms = new HashSet<>();
            for (Term term : tGuard.terms) {
                bTerms.add(new Variable(term.label));
            }
            lTGD.body.add(new PositiveAtom(fact.predicate, bTerms.stream().toList()));
            lTGD.head = new Conjunct();
            Set<Term> hTerms = new HashSet<>();
            for (Term term : tgd.head.getAtoms().get(0).terms) {
                hTerms.add(new Variable(term.label));
            }
            InMemoryDatabase tempDB = new InMemoryDatabase();
            int max = 0;
            Fact gFact = null;
            for (PositiveAtom atom : tgd.body.getAtoms()) {
                ArrayList<Term> ts = getTerms(atom, vMap);
                Fact bFact = new Fact(atom.predicate, ts);
                tempDB.addFact(bFact);
                if (bFact.terms.size() > max) {
                    max = bFact.terms.size();
                    gFact = bFact;
                }
            }
            PositiveAtom h = tgd.head.getAtoms().get(0);
            Fact hF = new Fact(h.predicate, getTerms(h, vMap));
            tempDB.addFact(hF);
            for (Shape side : fact.predicate.type.shapes) {
                ArrayList<Term> ts = new ArrayList<>();
                if (side.annotation.equals("1")) {
                    ts.add(gFact.terms.get(0));
                } else {
                    ts.add(gFact.terms.get(1));
                }
                tempDB.addFact(new Fact(side.predicate, ts));
            }

            OWLOntology ontology = reasoner.getRootOntology();
            Set<OWLAxiom> axiomsToAdd = new HashSet<>();
            addIndividualsToOntology2(tempDB, ontology, axiomsToAdd);
            Set<OWLAxiom> currentAxioms = ontology.getABoxAxioms(Imports.INCLUDED);
            Set<OWLAxiom> axiomsToRemove = new HashSet<>(currentAxioms);
            axiomsToRemove.removeAll(axiomsToAdd);
            ontologyManager.removeAxioms(ontology, axiomsToRemove);
            axiomsToAdd.removeAll(ontology.getABoxAxioms(Imports.INCLUDED));
            ontologyManager.addAxioms(ontology, axiomsToAdd);

//            removeIndividualsFromOntology(ontology);
//            addIndividualsToOntology(tempDB, ontology);
            OWLReasoner tempReasoner = createNewReasoner(ontology);
//            OWLReasoner tempReasoner = reasoner;

            Type type = tempDB.findType(hF, tempReasoner);
            tempReasoner.dispose();

            Predicate hPredicate = new Predicate(type);
            lTGD.head.add(new PositiveAtom(hPredicate, hTerms.stream().toList()));
            if (!result.tgds.contains(lTGD)) {
                Parser.addRulesOrFactsToProgram(result, lTGD.toString());
//                result.tgds.add(lTGD);
                List<Term> terms = new ArrayList<>();
                Fact f = new Fact(lTGD.head.getAtoms().get(0).predicate, terms);
                terms.add(new Constant("a"));
                if (f.predicate.arity == 2) {
                    if (hF.terms.get(0).equals(hF.terms.get(1)))
                        terms.add(new Constant("a"));
                    else
                        terms.add(new Constant("b"));
                }
                if (!visited.contains(f.toString())) {
                    String hpname = f.predicate.type.guard.predicate.name;
                    if (!newFacts.containsKey(hpname)) newFacts.put(hpname, new HashSet<>());
                    newFacts.get(hpname).add(f);
                }
            } else {
                logger.debug(lTGD.body);
                logger.debug(lTGD.head);
            }
        }
    }

    public static void addIndividualsToOntology2(InMemoryDatabase database, OWLOntology ontology, Set<OWLAxiom> axioms) {
        for (Fact fact : database.getFacts()) {
            if (fact.predicate.arity == 1)
                addIndividualToClassInOntology2(ontology, fact.terms.get(0).label, fact.predicate.name, axioms);
        }

        for (Fact fact : database.getFacts()) {
            if (fact.predicate.arity == 2)
                addIndividualsToRoles2(ontology, fact.terms.get(0).label, fact.terms.get(1).label, fact.predicate.name, axioms);
        }
    }


    public static void addIndividualsToOntology(InMemoryDatabase database, OWLOntology ontology) {
        for (Fact fact : database.getFacts()) {
            if (fact.predicate.arity == 1)
                addIndividualToClassInOntology(ontology, fact.terms.get(0).label, fact.predicate.name);
        }

        for (Fact fact : database.getFacts()) {
            if (fact.predicate.arity == 2)
                addIndividualsToRoles(ontology, fact.terms.get(0).label, fact.terms.get(1).label, fact.predicate.name);
        }
    }


    public static OWLReasoner createNewReasoner(OWLOntology ontology) {
        OWLReasoner result = reasonerFactory.createReasoner(ontology);
        result.precomputeInferences(InferenceType.OBJECT_PROPERTY_ASSERTIONS);
        return result;
    }

    public static void addIndividualsToRoles2(OWLOntology ontology, String subject, String object, String role, Set<OWLAxiom> axioms) {
        OWLObjectProperty property = getObjectPropertyFromOntology(ontology, role);
        OWLIndividual subjectIndividual = getIndividualsFromOntology(ontology, subject);
        if (subjectIndividual == null)
            subjectIndividual = dataFactory.getOWLNamedIndividual(IRI.create(IRI_PREFIX + subject));
        OWLIndividual objectIndividual = getIndividualsFromOntology(ontology, object);
        if (objectIndividual == null)
            objectIndividual = dataFactory.getOWLNamedIndividual(IRI.create(IRI_PREFIX + object));
        OWLObjectPropertyAssertionAxiom propertyAssertion = dataFactory.getOWLObjectPropertyAssertionAxiom(property, subjectIndividual, objectIndividual);
        axioms.add(propertyAssertion);
    }

    public static void addIndividualsToRoles(OWLOntology ontology, String subject, String object, String role) {
        OWLObjectProperty property = getObjectPropertyFromOntology(ontology, role);
        OWLIndividual subjectIndividual = getIndividualsFromOntology(ontology, subject);
        if (subjectIndividual == null)
            subjectIndividual = dataFactory.getOWLNamedIndividual(IRI.create(IRI_PREFIX + subject));
        OWLIndividual objectIndividual = getIndividualsFromOntology(ontology, object);
        if (objectIndividual == null)
            objectIndividual = dataFactory.getOWLNamedIndividual(IRI.create(IRI_PREFIX + object));
        OWLObjectPropertyAssertionAxiom propertyAssertion = dataFactory.getOWLObjectPropertyAssertionAxiom(property, subjectIndividual, objectIndividual);
        ontologyManager.addAxiom(ontology, propertyAssertion);
    }

    public static void addIndividualToClassInOntology2(OWLOntology ontology, String individualName, String className, Set<OWLAxiom> axioms) {
        OWLClass cls = getClassFromOntology(ontology, className);
        OWLNamedIndividual ind;
        if (individualMap.containsKey(individualName))
            ind = individualMap.get(individualName);
        else {
            ind = dataFactory.getOWLNamedIndividual(IRI.create(IRI_PREFIX + individualName));
            individualMap.put(individualName, ind);
        }

        OWLClassAssertionAxiom classAssertion;
        if (classAssertionAxiomMap.containsKey(individualName + "," + className))
            classAssertion = classAssertionAxiomMap.get(individualName + "," + className);
        else {
            classAssertion = dataFactory.getOWLClassAssertionAxiom(cls, ind);
            classAssertionAxiomMap.put(individualName + "," + className, classAssertion);
        }
        axioms.add(classAssertion);
    }

    public static void addIndividualToClassInOntology(OWLOntology ontology, String individualName, String className) {
        OWLClass cls = getClassFromOntology(ontology, className);
        OWLNamedIndividual ind;
        if (individualMap.containsKey(individualName))
            ind = individualMap.get(individualName);
        else {
            ind = dataFactory.getOWLNamedIndividual(IRI.create(IRI_PREFIX + individualName));
            individualMap.put(individualName, ind);
        }

        OWLClassAssertionAxiom classAssertion;
        if (classAssertionAxiomMap.containsKey(individualName + "," + className))
            classAssertion = classAssertionAxiomMap.get(individualName + "," + className);
        else {
            classAssertion = dataFactory.getOWLClassAssertionAxiom(cls, ind);
            classAssertionAxiomMap.put(individualName + "," + className, classAssertion);
        }
        ontologyManager.addAxiom(ontology, classAssertion);
    }

    public static OWLObjectProperty getObjectPropertyFromOntology(OWLOntology ontology, String name) {
        Optional<OWLObjectProperty> op = ontology.getObjectPropertiesInSignature().stream()
                .filter(o -> o.getIRI().getShortForm().equals(name)).findFirst();
        return op.orElse(null);
    }

    public static OWLClass getClassFromOntology(OWLOntology ontology, String name) {
        if (classMap.containsKey(name))
            return classMap.get(name);
        Optional<OWLClass> cls = ontology.getClassesInSignature().stream()
                .filter(c -> c.getIRI().getShortForm().equals(name)).findFirst();
        OWLClass res = cls.orElse(null);
        classMap.put(name, res);
        return res;
    }

    public static OWLNamedIndividual getIndividualsFromOntology(OWLOntology ontology, String name) {
        if (individualMap.containsKey(name))
            return individualMap.get(name);
        Optional<OWLNamedIndividual> individual = ontology.getIndividualsInSignature().stream()
                .filter(ind -> ind.getIRI().getShortForm().equals(name))
                .findFirst();
        OWLNamedIndividual res = individual.orElse(null);
        individualMap.put(name, res);
        return res;
    }

    private static ArrayList<Term> getTerms(PositiveAtom atom, Map<Variable, Term> vMap) {
        ArrayList<Term> ts = new ArrayList<>();
        for (Term term : atom.terms) {
            Variable v = (Variable) term;
            ts.add(vMap.get(v));
        }
        return ts;
    }

    private static Shape getShape(Atom atom) {
        Set<Term> set = new LinkedHashSet<>(atom.terms);
        ArrayList<Term> cTerms = new ArrayList<>(set);
        String annotation = "";
        for (Term term : atom.terms)
            annotation += (cTerms.indexOf(term) + 1);
//        annotation = annotation.substring(0, annotation.length() - 1);
        Shape shape = new Shape();
        shape.predicate = atom.predicate;
        shape.annotation = annotation;
        return shape;

    }

    private static List<Atom> getGuardAndSides(TGD tgd) {
        Conjunct body = tgd.body;
        List<Atom> atoms = new ArrayList<>();
        int max = 0;
        Atom guard = null;
        for (Atom atom : body.getAtoms())
            if (atom.terms.size() > max) {
                max = atom.terms.size();
                guard = atom;
            }
        atoms.add(guard);
        for (Atom atom : body.getAtoms())
            if (!atom.equals(guard))
                atoms.add(atom);
        return atoms;
    }

    public static void fillProgramDB(Program program, int domainSize, int record) {
        Random random = new Random();
        InMemoryDatabase db = (InMemoryDatabase) program.database;
        for (Predicate predicate : program.database.schema.predicates.values()) {
            char constant = 'c';
            int recordCount = 0;
            while (recordCount < record) {
                List<Term> terms = new ArrayList<>();
                for (int j = 0; j < predicate.arity; j++) {
                    terms.add(new Constant(constant + "" + (random.nextInt(domainSize) + 1)));
                }
                db.addFact(predicate, terms);
                recordCount = db.recordCount.get(predicate);
            }
        }
        logger.debug("db = " + db);
    }

    public static List<OWLAxiom> findAxiomsToRemove(OWLOntology ontology) {
        List<OWLAxiom> axiomsToRemove = new ArrayList<>();
        for (OWLEquivalentClassesAxiom axiom : ontology.getAxioms(AxiomType.EQUIVALENT_CLASSES)) {
            OWLClassExpression cls = axiom.getClassExpressionsAsList().get(1);
            if (cls instanceof OWLObjectIntersectionOf intersectionOf)
                for (OWLClassExpression cls1 : intersectionOf.getOperands())
                    if (cls1 instanceof OWLObjectSomeValuesFrom) {
                        axiomsToRemove.add(axiom);
                        break;
                    }
        }
        return axiomsToRemove;
    }

    public static void normalizeOntology(OWLOntology ontology) {
        int newConcepts = 1;
        String iri = ontology.getClassesInSignature().stream().toList().get(0).getIRI().getNamespace();

        for (OWLAxiom axiom : findAxiomsToRemove(ontology)) {
            ontologyManager.removeAxiom(ontology, axiom);
            OWLEquivalentClassesAxiom equivalentClassesAxiom = (OWLEquivalentClassesAxiom) axiom;
            List<OWLClassExpression> classExpressions = new ArrayList<>();
            for (OWLClassExpression classExpression : equivalentClassesAxiom.getClassExpressionsAsList()) {
                if (classExpression instanceof OWLObjectIntersectionOf intersectionOf) {
                    List<OWLClass> classes = new ArrayList<>();
                    for (OWLClassExpression expression : intersectionOf.getOperands()) {
                        if (expression instanceof OWLObjectSomeValuesFrom someValuesFrom) {
                            OWLClass newClass = dataFactory.getOWLClass(IRI.create(iri + "Z" + newConcepts));
                            classes.add(newClass);
                            newConcepts++;
                            OWLDeclarationAxiom declarationAxiom = dataFactory.getOWLDeclarationAxiom(newClass);
                            ontologyManager.addAxiom(ontology, declarationAxiom);
                            OWLEquivalentClassesAxiom newEquivalentClassesAxiom = dataFactory.getOWLEquivalentClassesAxiom(newClass, someValuesFrom);
                            ontologyManager.addAxiom(ontology, newEquivalentClassesAxiom);
                        } else
                            classes.add(expression.asOWLClass());
                    }
                    while (classes.size() >= 2) {
                        OWLClass cls1 = classes.get(0);
                        OWLClass cls2 = classes.get(1);
                        classes.remove(cls1);
                        classes.remove(cls2);
                        OWLClass newClass = dataFactory.getOWLClass(IRI.create(iri + "Z" + newConcepts));
                        OWLDeclarationAxiom declarationAxiom = dataFactory.getOWLDeclarationAxiom(newClass);
                        classes.add(newClass);
                        newConcepts++;
                        OWLObjectIntersectionOf intersection = dataFactory.getOWLObjectIntersectionOf(cls1, cls2);
                        ontologyManager.addAxiom(ontology, declarationAxiom);
                        OWLEquivalentClassesAxiom equivalentClassAxiom = dataFactory.getOWLEquivalentClassesAxiom(newClass, intersection);
                        ontologyManager.addAxiom(ontology, equivalentClassAxiom);
                    }
                    classExpressions.add(classes.get(0));
                } else
                    classExpressions.add(classExpression);
            }
            OWLEquivalentClassesAxiom equivalentClassAxiom = dataFactory.getOWLEquivalentClassesAxiom(classExpressions.get(0).asOWLClass(),
                    classExpressions.get(1).asOWLClass());
            ontologyManager.addAxiom(ontology, equivalentClassAxiom);
        }
    }

    public static <T> List<List<T>> getUnionOfSubsets(
            List<List<T>> set1, List<List<T>> set2) {
        List<List<T>> unions = new ArrayList<>();

        for (List<T> subset1 : set1) {
            for (List<T> subset2 : set2) {
                List<T> union = new ArrayList<>();
                union.addAll(subset1);
                union.addAll(subset2);
                unions.add(union);
            }
        }

        return unions;
    }

    public static void findTypes(List<Predicate> predicates, List<List<Predicate>> unarySubsets,
                                 Map<String, List<Type>> types, String annotation) {
        for (Predicate predicate : predicates) {
            types.put(predicate.name, new ArrayList<>());
            Shape guardShape = new Shape();
            guardShape.annotation = annotation;
            guardShape.predicate = predicate;
            for (List<Predicate> subset : unarySubsets) {
                if (!subset.contains(predicate)) {
                    Set<Shape> shapes = new TreeSet<>(toStringComparator);
                    for (Predicate p : subset) {
                        Shape shape = new Shape();
                        shape.annotation = "1";
                        shape.predicate = p;
                        shapes.add(shape);
                    }
                    Type type = new Type();
                    type.guard = guardShape;
                    type.shapes = shapes;
                    types.get(predicate.name).add(type);
                }
            }
        }
    }

    public static void findUnaryTypes(List<Predicate> unaryPredicates, List<List<Predicate>> unarySubsets,
                                      Map<String, List<Type>> types) {
        findTypes(unaryPredicates, unarySubsets, types, "1");
    }

    public static void findBinaryTypes(List<Predicate> binaryPredicates, List<List<Predicate>> unarySubsets,
                                       Map<String, List<Type>> types, List<List<LabeledPredicate>> l1,
                                       List<List<LabeledPredicate>> l2) {

        findTypes(binaryPredicates, unarySubsets, types, "1,1");
//        List<List<LabeledPredicate>> union = getUnionOfSubsets(l1, l2);
        System.out.println(binaryPredicates.size());
        for (Predicate predicate : binaryPredicates) {
            System.out.println(binaryPredicates.size());
            Shape guardShape = new Shape();
            guardShape.annotation = "1,2";
            guardShape.predicate = predicate;
            for (List<LabeledPredicate> subset : l1) {
                Set<Shape> shapes = new TreeSet<>(toStringComparator);
                for (LabeledPredicate p : subset) {
                    Shape shape = new Shape();
                    shape.annotation = p.label;
                    shape.predicate = p;
                    shapes.add(shape);
                }
                Type type = new Type();
                type.guard = guardShape;
                type.shapes = shapes;
                types.get(predicate.name).add(type);
            }
        }

    }


    public static Program blindLinearize(Program program) {
        Program result = new Program();
        result.database = program.database;

        List<Predicate> unaryPredicates = result.database.schema.predicates.values().stream().filter(p -> p.arity == 1).toList();
        List<Predicate> binaryPredicates = result.database.schema.predicates.values().stream().filter(p -> p.arity == 2).toList();

        List<LabeledPredicate> unaryLabeledPredicates1 = new ArrayList<>();
        System.out.println("HI");
        for (Predicate p : unaryPredicates) {
            unaryLabeledPredicates1.add(new LabeledPredicate("1", p));
            unaryLabeledPredicates1.add(new LabeledPredicate("2", p));
        }
        System.out.println("HI2");

        List<List<Predicate>> unarySubset = getUnarySubsets(unaryPredicates);
        System.out.println("HI3");
        List<List<LabeledPredicate>> unaryLabeledSubset1 = getUnarySubsets(unaryLabeledPredicates1);
        System.out.println(unaryLabeledSubset1.size());
        System.out.println("HI4");

        Map<String, List<Type>> types = new HashMap<>();
        findUnaryTypes(unaryPredicates, unarySubset, types);
        System.out.println("HI5");
        findBinaryTypes(binaryPredicates, unarySubset, types, unaryLabeledSubset1, unaryLabeledSubset1);

        return result;
    }

    public static void main(String[] args) throws
            SQLException, OWLOntologyCreationException, IOException, OWLOntologyStorageException {

        File file = new File(args[0]);
        String path = file.getAbsolutePath();
        String resPath = path.substring(0, path.length() - 3) + "res";
        File outfile = new File(resPath);
        outfile.createNewFile();
        double s1 = System.nanoTime();
        logger.info("Reading ontology... " + file.getName());
        double startTime = System.nanoTime();
        OWLOntology ontology = ontologyManager.loadOntologyFromOntologyDocument(file);
        double endTime = System.nanoTime();
        double time = (endTime - startTime) / 1000000F;
        logger.info("Time [" + time + "]");

        logger.info("Normalization...");
        startTime = System.nanoTime();
        normalizeOntology(ontology);
        endTime = System.nanoTime();
        time = (endTime - startTime) / 1000000F;
        logger.info("Time [" + time + "]");

        Program program = generateProgram(ontology);
        ignore.forEach(s -> program.database.schema.predicates.remove(s));
        fillProgramDB(program, 1, 1);
        addIndividualsToOntology((InMemoryDatabase) program.database, ontology);
        OntologyAnalyzer.addProgramStats(program, null);
//
        FileWriter out = new FileWriter(outfile, true);
        OntologyAnalyzer.exportResults(out, program.stats, "");
//
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
//
        logger.info("Linearizing database...");
        startTime = System.nanoTime();
        InMemoryDatabase lDB = linearize((InMemoryDatabase) program.database, reasoner);
        endTime = System.nanoTime();
        double tRules = (endTime - startTime) / 1000000F;
        logger.info("Time [" + tRules + "]");
//
        logger.info("Linearizing rules...");
        startTime = System.nanoTime();
        Program lProgram = linearize(program, lDB, reasoner);
//        Program lProgram = blindLinearize(program);
        endTime = System.nanoTime();
        lProgram.stats.put(OntologyAnalyzer.TIME_LINEAR_DB, tRules);
        time = (endTime - startTime) / 1000000F;
        lProgram.stats.put(OntologyAnalyzer.TIME_LINEAR_RULES, time);
        logger.info("Time [" + time + "]");
//
//
        logger.info("Termination analysis...");
        startTime = System.nanoTime();
        OntologyAnalyzer.analyzeProgramTermination(lProgram, true);
        OntologyAnalyzer.exportResults(out, lProgram.stats, "l");
        out.close();
        endTime = System.nanoTime();
        time = (endTime - startTime) / 1000000F;
        logger.info("Time [" + time + "]");
        double e1 = System.nanoTime();
        logger.info("Total time [" + (e1 - s1) / 1000000F + "]");
    }

    public static void main1(String[] args) throws
            SQLException, OWLOntologyCreationException, IOException, OWLOntologyStorageException {

        File dir = new File(args[0]);
        File[] files = dir.listFiles((dir1, name) -> name.endsWith(".owl"));
        Arrays.sort(files, (o1, o2) -> {
            long size1 = o1.length();
            long size2 = o2.length();
            return Long.compare(size1, size2);
        });
        System.out.println(files);
        for (File file : files) {
            String path = file.getAbsolutePath();
            String resPath = path.substring(0, path.length() - 3) + "res";
            File outfile = new File(resPath);
            if (outfile.exists()) continue;
            outfile.createNewFile();
            logger.info("Reading ontology... " + file.getName());
            double startTime = System.nanoTime();
            OWLOntology ontology = ontologyManager.loadOntologyFromOntologyDocument(file);
            double endTime = System.nanoTime();
            double time = (endTime - startTime) / 1000000F;
            logger.info("Time [" + time + "]");

            logger.info("Normalization...");
            startTime = System.nanoTime();
            normalizeOntology(ontology);
            endTime = System.nanoTime();
            time = (endTime - startTime) / 1000000F;
            logger.info("Time [" + time + "]");

            System.out.println("Program generation...");
            Program program = generateProgram(ontology);
            System.out.println("Filling edb...");
            fillProgramDB(program, 1, 1);
            System.out.println("Updating ontologies...");
            addIndividualsToOntology((InMemoryDatabase) program.database, ontology);
            OntologyAnalyzer.addProgramStats(program, null);
//
            FileWriter out = new FileWriter(outfile, true);
            OntologyAnalyzer.exportResults(out, program.stats, "");
//
            System.out.println("Creating the reasoner...");
            OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
            reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

            logger.info("Linearizing database...");
            startTime = System.nanoTime();
            InMemoryDatabase lDB = linearize((InMemoryDatabase) program.database, reasoner);
            endTime = System.nanoTime();
            double tRules = (endTime - startTime) / 1000000F;
            logger.info("Time [" + tRules + "]");

            logger.info("Linearizing rules...");
            startTime = System.nanoTime();
            Program lProgram = linearize(program, lDB, reasoner);
//            lProgram.database = lDB;
            endTime = System.nanoTime();
            lProgram.stats.put(OntologyAnalyzer.TIME_LINEAR_DB, tRules);
            time = (endTime - startTime) / 1000000F;
            lProgram.stats.put(OntologyAnalyzer.TIME_LINEAR_RULES, time);
            logger.info("Time [" + time + "]");


            logger.info("Termination analysis...");
            startTime = System.nanoTime();
            OntologyAnalyzer.analyzeProgramTermination(lProgram, true);
            OntologyAnalyzer.exportResults(out, lProgram.stats, "l");
            out.close();
            endTime = System.nanoTime();
            time = (endTime - startTime) / 1000000F;
            logger.info("Time [" + time + "]");
            System.out.println("------------------------------------------------------------------------------");
        }
    }
}

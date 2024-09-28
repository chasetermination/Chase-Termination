package ca.chaseTermination.engine;

import ca.chaseTermination.primitives.Fact;
import ca.chaseTermination.primitives.Predicate;
import ca.chaseTermination.primitives.Term;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;

import java.util.*;

public class InMemoryDatabase extends Database {
    private Map<String, Fact> facts = new HashMap<>();
    Comparator<Shape> toStringComparator = Comparator.comparing(Shape::toString);


    private static final List<String> ignore = List.of("DbXref", "Definition", "ObsoleteClass", "Subset", "Synonym", "SynonymType");

    private Map<String, OWLNamedIndividual> individualMap = new HashMap<>();
    Map<String, Set<Shape>> currentTypes = new HashMap<>();

    public InMemoryDatabase() {

    }

    public Fact[] getFacts() {
        Object[] array = facts.values().toArray();
        return Arrays.copyOf(array, array.length, Fact[].class);
    }


    public Fact addFact(Predicate predicate, List<Term> terms) {
        Fact f = new Fact(predicate, terms);
        return addFact(f);
    }

    private void incrementRecordCount(Predicate predicate) {
        if (!recordCount.containsKey(predicate)) recordCount.put(predicate, 0);
        int count = recordCount.get(predicate);
        recordCount.put(predicate, count + 1);
    }

    public Fact addFact(Fact fact) {
        String s = fact.toString();
        if (!facts.containsKey(s)) {
            facts.put(s, fact);
            incrementRecordCount(fact.predicate);
        }
        Fact f = facts.get(s);
        schema.addPredicate(f.predicate);
        return fact;
    }

    @Override
    public Map<Predicate, Set<String>> findShapes() {
        Map<Predicate, Set<String>> shapes = new HashMap<>();
        for (Fact f : facts.values()) {
            Predicate predicate = f.predicate;
            List<Term> terms = f.terms;
            shapes.computeIfAbsent(predicate, k -> new HashSet<>());
            StringBuilder ann = getShapes(terms);
            Set<String> predicateShapes = shapes.get(predicate);
            predicateShapes.add(ann.toString());
            shapes.put(predicate, predicateShapes);
        }
        return shapes;
    }

    private StringBuilder getShapes(List<Term> terms) {
        ArrayList<String> values = new ArrayList<>();
        StringBuilder ann = new StringBuilder();
        int max = 1;
        for (Term term : terms) {
            String newString = term.label;
            if (values.contains(newString))
                ann.append(Integer.toHexString((values.indexOf(newString) + 1)));
            else {
                ann.append(Integer.toHexString(max));
                max++;
                values.add(newString);
            }
        }
        return ann;
    }

    @Override
    public void printDBStats() {
        //TODO
    }



    public boolean isEmpty(Predicate predicate) {
        return !recordCount.containsKey(predicate) || recordCount.get(predicate) == 0;
    }

    @Override
    public Set<Type> findTypes(OWLOntology ontology) {
        OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
        HashSet<Type> types = new HashSet<>();
        for (Fact fact : facts.values())
            types.add(findType(fact, reasoner));
        return types;
    }

    public Type findType(Fact fact, OWLReasoner reasoner) {
        Type type = new Type();
        Shape guard = new Shape();
        guard.predicate = fact.predicate;
        guard.annotation = (fact.terms.size() == 1 ? "1" : (fact.terms.get(0).label.equals(fact.terms.get(1).label) ? "11" : "12"));

        Set<Shape> shapes = new HashSet<>();

        List<Term> terms;
        terms = new ArrayList<>(fact.terms);
        terms.sort(Comparator.comparing((Term o) -> o.label));
        String key = terms.toString();
        if (currentTypes.containsKey(key)) {
            type.shapes = currentTypes.get(key);
        }
        else {
            findTypeForIndividuals(reasoner, fact, shapes);
            type.shapes = new TreeSet<>(toStringComparator);
            type.shapes.addAll(shapes);
            currentTypes.put(key, type.shapes);
        }
        type.guard = guard;
        shapes.remove(type.guard);
        return type;
    }

    private void findTypeForIndividuals(OWLReasoner reasoner, Fact fact, Set<Shape> shapes) {
        int i = 1;
        for (Term term : fact.terms) {
            OWLNamedIndividual individual = individualMap.get(term.label);
            if(individual == null) {
                individual = OntologyManager.getIndividualsFromOntology(reasoner, term.label);
                individualMap.put(term.label, individual);
            }
            NodeSet<OWLClass> classesA = reasoner.getTypes(individual, false);
            for (OWLClass cls : classesA.getFlattened()) {
                if (ignore.contains(cls.getIRI().getShortForm()))
                    continue;
                if (!cls.getIRI().getShortForm().equals("Thing"))
                    addNewShape(cls.getIRI().getShortForm(), Integer.toString(i), shapes, 1);
            }
            i++;
            if (fact.terms.size() == 2 && fact.terms.get(0).equals(fact.terms.get(1)))
                break;
        }
    }

    private void addNewShape(String predicate, String annotation, Set<Shape> shapes, int arity) {
        Shape shape = new Shape();
        Predicate p = schema.predicates.get(predicate);
        shape.predicate =  p != null ? p : new Predicate(predicate, arity);
        schema.addPredicate(shape.predicate);
        shape.annotation = annotation;
        shapes.add(shape);
    }
}

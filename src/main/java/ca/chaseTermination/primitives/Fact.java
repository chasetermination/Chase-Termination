package ca.chaseTermination.primitives;

import java.util.List;

public class Fact extends PositiveAtom {
    public Fact(Predicate p, List<Term> ts) {
        super(p, ts);
    }


    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return toString().equals(obj.toString());
    }

    @Override
    public String toString() {
        String s = predicate.name + "(";
        for (Term term : terms) {
            s += term.label + ",";
        }
        s = s.substring(0, s.length()-1) + ")";
        return s;
    }
}

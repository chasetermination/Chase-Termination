package ca.chaseTermination.primitives;

import java.util.Objects;

public class LabeledPredicate extends Predicate {

    public String label;

    public LabeledPredicate(String label, Predicate p) {
        super(p.name, p.arity);
        this.label = label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LabeledPredicate predicate = (LabeledPredicate) o;
        return arity == predicate.arity && Objects.equals(name, predicate.name) && Objects.equals(label, predicate.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, arity, label);
    }
}

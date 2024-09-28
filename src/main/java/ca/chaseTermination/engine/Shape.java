package ca.chaseTermination.engine;

import ca.chaseTermination.primitives.Predicate;

public class Shape {
    private static final String DELIMITER = "@";
    public String annotation;
    public Predicate predicate;

    @Override
    public String toString() {
        return predicate.name + DELIMITER + "(" + annotation + ")";
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (predicate.name + DELIMITER + annotation).hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Shape other = (Shape) o;
        return (predicate.name + DELIMITER + annotation).equals((other.predicate.name + DELIMITER + other.annotation));
    }
}

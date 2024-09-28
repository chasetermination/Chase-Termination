package ca.chaseTermination.engine;

import java.util.Set;

public class Type {
    public Shape guard;
    public Set<Shape> shapes;

    @Override
    public String toString() {
        String s = guard.toString();
        for (Shape shape : shapes)
            s += shape.toString();
        return s;
    }

    public int findArity() {
        return guard.annotation.length();
    }
}

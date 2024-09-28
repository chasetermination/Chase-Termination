package ca.chaseTermination.primitives;

import ca.chaseTermination.engine.Program;
import ca.chaseTermination.engine.Shape;
import ca.chaseTermination.engine.Type;

import java.util.Objects;

public class Predicate {
    public String name;
    public int arity;
    public Program program;

    public Type type;


    public boolean extensional = false;

    public Predicate(String name, int arity, Program program) {
        this.name = name;
        this.arity = arity;
        this.program = program;
    }

    public Predicate(String name, int arity) {
        this.name = name;
        this.arity = arity;
    }

    public Predicate(Type type) {
        StringBuilder builder = new StringBuilder();
        builder.append(type.guard.predicate.name);
        builder.append("@");
        builder.append(type.guard.annotation);
//        name = type.guard.predicate.name + "@" + type.guard.annotation;
        for (Shape shape : type.shapes) {
            builder.append("&");
            builder.append(shape.predicate.name);
            builder.append("@");
            builder.append(shape.annotation);
//            name += "&" + shape.predicate.name + "@" + shape.annotation;
        }
        name = builder.toString();
        this.type = type;
        this.arity = type.findArity();

    }

    @Override
    public String toString() {
        return name;
    }

    public boolean isAdorned() {
        return name.contains("^");
    }

    public String getAdornment() {
        if (!isAdorned())
            return null;
        return name.substring(name.indexOf("^") + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Predicate predicate = (Predicate) o;
        return arity == predicate.arity && Objects.equals(name, predicate.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, arity);
    }
}

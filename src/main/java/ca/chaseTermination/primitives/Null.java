package ca.chaseTermination.primitives;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Null extends Term {
    public static int INDEX = 1;
    public static final String LABEL = "N";
    public static Set<Term> nullSet = new HashSet<>();
    private static Map<String, Null> nulls = new HashMap<>();
    public boolean frozen = false;
    public boolean confirmed = false;
    public int index = 0;

    private Null(String label, int index) {
        this.label = label;
        this.index = index;
    }


    public Null() {
        this.label = LABEL + INDEX;
        index = INDEX;
        INDEX++;
    }

    static Null fetch(String s) {
        if (!nulls.containsKey(s))
            throw new RuntimeException("Invalid null label (" + s + ")");
        return nulls.get(s);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Null && toString().equals(o.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}

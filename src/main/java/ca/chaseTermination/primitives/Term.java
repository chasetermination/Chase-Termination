package ca.chaseTermination.primitives;

import ca.chaseTermination.engine.Program;
import ca.chaseTermination.parser.Parser;

import java.util.Objects;

public abstract class Term {
    public String label;

    public static Term parse(String s, boolean body, Program program, Rule... owner) {
        Term term;
        s = Parser.sanitizePredicateName(s);
        if (s.startsWith("z_"))
            term = Null.fetch(s);
        else if (s.contains("'") || owner == null || owner.length == 0)
            term = program.database.schema.fetchConstant(s.replaceAll("'", "\""));
        else if (s.equals(Variable.DONT_CARE))
            term = owner[0].getDontCare();
        else
            term = owner[0].fetchVariable(s, body);
        return term;
    }

    public String toString() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Term term = (Term) o;
        return Objects.equals(label, term.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label);
    }
}

import cs132.IR.token.Identifier;

public class Var {
    final Identifier id;

    Var(Identifier id) {
        this.id = id;
    }

    boolean nameEquals(Var other) {
        return toString().equals(other.toString());
    }

    boolean nameEquals(Identifier other) {
        return toString().equals(other.toString());
    }

    @Override
    public String toString() {
        return id.toString();
    }
}

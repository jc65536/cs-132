import cs132.IR.token.Identifier;

public class NodeInOut {
    final CFNode node;
    final List<Identifier> in;
    final List<Identifier> out;

    NodeInOut(CFNode node, List<Identifier> in, List<Identifier> out) {
        this.node = node;
        this.in = in;
        this.out = out;
    }

    @Override
    public String toString() {
        return String.format("%s\n  In: %s\n  Out: %s\n", node.ins,
                in.strJoin(", "),
                out.strJoin(", "));
    }
}


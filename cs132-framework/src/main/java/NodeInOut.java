public class NodeInOut {
    final CFNode node;
    final List<Var> in;
    final List<Var> out;

    NodeInOut(CFNode node, List<Var> in, List<Var> out) {
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


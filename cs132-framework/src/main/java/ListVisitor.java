import java.util.*;

import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

// Visitor for a sequence of nodes; The advantage of using ListVisitor vs.
// nodes.stream().map() is that this generates a lazy list compatible with the
// rest of the lists I use, instead of a Java stream.

public class ListVisitor<R, A> extends GJDepthFirst<List<R>, A> {
    final GJDepthFirst<R, A> v;

    ListVisitor(GJDepthFirst<R, A> v) {
        this.v = v;
    }

    @Override
    public List<R> visit(NodeOptional n, A argu) {
        if (n.present())
            return n.node.accept(this, argu);
        else
            return List.nul();
    }

    @Override
    public List<R> visit(ExpressionList n, A argu) {
        return new List<>(() -> Optional.of(new Pair<>(n.f0.accept(v, argu), n.f1.accept(this, argu))));
    }

    @Override
    public List<R> visit(FormalParameterList n, A argu) {
        return new List<>(() -> Optional.of(new Pair<>(n.f0.accept(v, argu), n.f1.accept(this, argu))));
    }

    @Override
    public List<R> visit(NodeListOptional n, A argu) {
        return mkList(n.nodes.iterator(), argu);
    }

    List<R> mkList(Iterator<Node> it, A argu) {
        if (it.hasNext())
            return new List<>(() -> Optional.of(new Pair<>(it.next().accept(v, argu), mkList(it, argu))));
        else
            return List.nul();
    }
}

import java.util.*;

import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

public class ListVisitor<R, A> extends GJDepthFirst<List<R>, A> {
    final GJDepthFirst<R, A> visitor;

    public ListVisitor(GJDepthFirst<R, A> v) {
        this.visitor = v;
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
        return visitList(n.f0, n.f1.nodes, argu);
    }

    @Override
    public List<R> visit(FormalParameterList n, A argu) {
        return visitList(n.f0, n.f1.nodes, argu);
    }

    List<R> visitList(Node first, Vector<Node> rest, A argu) {
        return rest.stream().reduce(List.of(first.accept(visitor, argu)),
            (acc, node) -> acc.cons(node.accept(visitor, argu)), (u, v) -> v);
    }
}

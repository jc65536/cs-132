import java.util.*;

import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

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

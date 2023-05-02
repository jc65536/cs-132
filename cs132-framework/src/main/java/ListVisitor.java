import java.util.*;
import java.util.function.*;

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
        return new List<>(() -> new Pair<>(n.f0.accept(v, argu), n.f1.accept(this, argu)));
    }

    @Override
    public List<R> visit(FormalParameterList n, A argu) {
        return new List<>(() -> new Pair<>(n.f0.accept(v, argu), n.f1.accept(this, argu)));
    }

    @Override
    public List<R> visit(NodeListOptional n, A argu) {
        return visitList(n.nodes, argu);
    }

    List<R> visitList(Vector<Node> nodes, A argu) {
        final var it = nodes.iterator();
        final var s = new Lazy<Supplier<_List<R>>>(z -> () -> {
            if (it.hasNext()) {
                final var r = it.next().accept(v, argu);
                return new Pair<>(r, z.get());
            } else {
                return new Null<>();
            }
        }).get();
        return new List<>(s);
    }
}

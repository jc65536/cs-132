import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

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
        return mkList(n.nodes.iterator(), argu);
    }

    List<R> mkList(Iterator<Node> it, A argu) {
        if (it.hasNext())
            return new List<>(() -> new Pair<>(it.next().accept(v, argu), mkList(it, argu)));
        else
            return List.nul();
    }
}

class FoldVisitor<R, M, A> extends GJDepthFirst<R, R> {
    final GJDepthFirst<M, ? super R> v;
    final BiFunction<R, M, R> f;

    FoldVisitor(GJDepthFirst<M, ? super R> v, BiFunction<R, M, R> f) {
        this.v = v;
        this.f = f;
    }

    @Override
    public R visit(NodeOptional n, R argu) {
        if (n.present())
            return n.node.accept(this, argu);
        else
            return argu;
    }

    @Override
    public R visit(ExpressionList n, R argu) {
        return n.f1.accept(this, f.apply(argu, n.f0.accept(v, argu)));
    }

    @Override
    public R visit(FormalParameterList n, R argu) {
        return n.f1.accept(this, f.apply(argu, n.f0.accept(v, argu)));
    }

    @Override
    public R visit(NodeListOptional n, R argu) {
        return n.nodes.stream().reduce(argu, (r, node) -> f.apply(r, node.accept(v, r)), (u, v) -> v);
    }
}

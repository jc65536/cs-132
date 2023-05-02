import java.util.*;
import java.util.function.*;

import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

public class ListVisitor<R, M, A> extends GJDepthFirst<Optional<List<R>>, A> {
    final GJDepthFirst<M, A> v;
    final BiFunction<List<R>, M, Optional<? extends R>> chk;

    ListVisitor(GJDepthFirst<M, A> v, BiFunction<List<R>, M, Optional<? extends R>> chk) {
        this.v = v;
        this.chk = chk;
    }

    @Override
    public Optional<List<R>> visit(NodeOptional n, A argu) {
        if (n.present())
            return n.node.accept(this, argu);
        else
            return Optional.of(List.nul());
    }

    @Override
    public Optional<List<R>> visit(ExpressionList n, A argu) {
        return visitList(Optional.of(n.f0), n.f1.nodes, argu);
    }

    @Override
    public Optional<List<R>> visit(FormalParameterList n, A argu) {
        return visitList(Optional.of(n.f0), n.f1.nodes, argu);
    }

    @Override
    public Optional<List<R>> visit(NodeListOptional n, A argu) {
        return visitList(Optional.empty(), n.nodes, argu);
    }

    Optional<List<R>> visitList(Optional<Node> initNode, Vector<Node> vec, A argu) {
        final var init = initNode
                .flatMap(n -> chk.apply(List.nul(), n.accept(v, argu)))
                .map(List::<R>of)
                .or(() -> Optional.of(List.nul()));

        return vec.stream().reduce(init, (accOpt, node) -> accOpt
                .flatMap(acc -> chk.apply(acc, node.accept(v, argu))
                        .map(r -> acc.cons(r))),
                (u, v) -> v);
    }
}

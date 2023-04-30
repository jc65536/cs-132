import java.util.*;
import java.util.function.*;

import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

public class ListVisitor<R, A> extends GJDepthFirst<List<R>, A> {
    final GJDepthFirst<R, A> v;
    final BiPredicate<List<R>, R> chk;
    final String msg;

    public ListVisitor(GJDepthFirst<R, A> v, BiPredicate<List<R>, R> chk, String msg) {
        this.v = v;
        this.chk = chk;
        this.msg = msg;
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
        return visitList(List.of(n.f0.accept(v, argu)), n.f1.nodes, argu);
    }

    @Override
    public List<R> visit(FormalParameterList n, A argu) {
        return visitList(List.of(n.f0.accept(v, argu)), n.f1.nodes, argu);
    }

    @Override
    public List<R> visit(NodeListOptional n, A argu) {
        return visitList(List.nul(), n.nodes, argu);
    }

    List<R> visitList(List<R> init, Vector<Node> vec, A argu) {
        return vec.stream().reduce(init, (acc, node) -> {
            final var res = node.accept(v, argu);
            if (chk.test(acc, res))
                return acc.cons(res);
            else
                return Util.error(msg);
        }, (u, v) -> v);
    }
}

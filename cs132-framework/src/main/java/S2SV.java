import java.util.function.*;

import cs132.IR.SparrowParser;
import cs132.IR.visitor.SparrowConstructor;

public class S2SV {
    // TODO: out for return
    static List<NodeInOut> fp(int inOutTotal, List<NodeInOut> curr) {
        final var nextRec = curr.fold(new T2<>(0, List.<NodeInOut>nul()), (acc, nio) ->
            acc.then(tot -> list -> {
                final var newIn = nio.node.use.join(nio.out.filter(v -> !nio.node.def.exists(v::nameEquals))).unique(Var::nameEquals);
                final var newOut = nio.node.succ.flatMap(s -> curr.find(t -> t.node == s).get().in).unique(Var::nameEquals);
                final var term = newIn.count() + newOut.count();
                return new T2<>(tot + term, list.cons(new NodeInOut(nio.node, newIn, newOut)));
            }));
        if (nextRec.a != inOutTotal) {
            return fp(nextRec.a, nextRec.b);
        } else {
            return curr;
        }
    }

    public static void main(String[] args) throws Exception {
        new SparrowParser(System.in);
        final var ctor = new SparrowConstructor();
        SparrowParser.Program().accept(ctor);
        final var prgm = ctor.getProgram();
        final var fns = List.fromJavaList(prgm.funDecls);

        final var cfGraphs = fns.map(fn -> {
            final var cfGraph = new Lazy<T2<List<CFNode>, List<LabelNode>>>(
                    z -> fn.accept(new CFGraphVisitor(), T2.unwrap(z))).get().a;

            final var k = cfGraph.map(n -> new NodeInOut(n, List.nul(), List.nul()));

            final var fixed = fp(0, k);

            System.out.println("Function " + fn.functionName.name);
            System.out.println(fixed.fold("", (acc, n) -> acc + n));

            return fixed;
        }).reverse();
    }
}

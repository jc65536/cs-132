import java.util.function.*;

import cs132.IR.SparrowParser;
import cs132.IR.visitor.SparrowConstructor;

public class S2SV {
    public static void main(String[] args) throws Exception {
        new SparrowParser(System.in);
        final var ctor = new SparrowConstructor();
        SparrowParser.Program().accept(ctor);
        final var prgm = ctor.getProgram();
        final var fns = List.fromJavaList(prgm.funDecls);

        final var cfGraphs = fns.map(fn -> {
            final var cfGraph = new Lazy<T2<List<CFNode>, List<LabelNode>>>(
                    z -> fn.accept(new CFGraphVisitor(), T2.unwrap(z))).get().a;

            System.out.println("Function " + fn.functionName.name);
            System.out.println(cfGraph.fold("", (acc, n) -> acc + n));

            return 0;
        }).reverse();
    }
}

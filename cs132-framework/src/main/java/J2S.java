import java.util.*;

import cs132.IR.sparrow.*;
import cs132.IR.token.*;
import cs132.IR.token.Identifier;
import cs132.minijava.*;
import cs132.minijava.syntaxtree.MainClass;

public class J2S {
    static Instruction comment(String s) {
        return new Move_Id_Integer(new Identifier("__COMMENT__" + s), 0);
    }

    static FunctionDecl transMain(MainClass main, TypeEnv typeEnv) {
        final var statSize = typeEnv.vtables
                .fold(Optional.<Vtable>empty(), (mvtOpt, vt) -> mvtOpt
                        .filter(mvt -> mvt.offset > vt.offset)
                        .or(() -> Optional.of(vt)))
                .map(vt -> vt.offset + vt.size)
                .orElse(0);

        return new TransEnv(List.nul(), 0).genSym((tmp, transEnv) -> {
            final var allocStat = transEnv
                    .cons(new Move_Id_Integer(tmp, statSize))
                    .cons(new Alloc(TransEnv.stat, tmp));

            final var writeVtables = typeEnv.vtables.fold(allocStat,
                    (acc, vt) -> vt.write(TransEnv.stat, tmp, acc));

            final var locals = main.f14.accept(new ListVisitor<>(new LocalVisitor()), typeEnv);
            final var localsEnv = typeEnv.addLocals(locals);

            final var bodyEnv = main.f15.nodes.stream().reduce(writeVtables.initLocals(locals),
                    (acc, n) -> n.accept(new StmtVisitor(), new T2<>(localsEnv, acc)),
                    (u, v) -> v);

            return new FunctionDecl(new FunctionName("main"), java.util.List.of(),
                    new Block(bodyEnv.codeRev.reverse().toJavaList(), TransEnv.stat));
        });
    }

    public static void main(String[] args) throws Exception {
        new MiniJavaParser(System.in);
        final var root = MiniJavaParser.Goal();

        final var env = new Lazy<TypeEnv>(z -> {
            final var classes = root.f1
                    .accept(new ListVisitor<>(new ClassVisitor()), z)
                    .fold(new T2<>(new Lazy<>(() -> 0), List.<Class>nul()),
                            (acc, mkClass) -> {
                                final var vtableOffset = acc.a;
                                final var classAcc = acc.b;
                                final var cls = mkClass.apply(vtableOffset);
                                return new T2<>(cls.nextVtableOffset, classAcc.cons(cls));
                            }).b;
            return new TypeEnv(List.nul(), classes, Optional.empty());
        }).get();

        final var funs = env.classes.flatMap(c -> c.methods
                .map(m -> m.translate(env.enterClass(c))))
                .cons(transMain(root.f0, env));

        final var prgm = new Program(funs.toJavaList());

        System.out.println(prgm.toString());
    }
}

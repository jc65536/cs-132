import java.util.*;

import cs132.IR.sparrow.*;
import cs132.IR.token.*;
import cs132.IR.token.Identifier;
import cs132.minijava.*;
import cs132.minijava.syntaxtree.MainClass;

public class J2S {
    static Instruction comment(String s) {
        return new Move_Id_Integer(new Identifier("________________________" + s), 0);
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
                    (acc, vt) -> vt.write(TransEnv.stat, tmp, acc))
                    .cons(comment("End_vtables"));

            final var locals = main.f14.accept(new ListVisitor<>(new LocalVisitor()), typeEnv);
            final var localsEnv = typeEnv.addLocals(locals);

            return main.f15.accept(new FoldVisitor<>(new StmtVisitor(), T2::setB),
                    new T2<>(localsEnv, writeVtables.initLocals(locals)))
                    .consume((u, body) -> new FunctionDecl(new FunctionName("main"), java.util.List.of(),
                            new Block(body.codeRev.reverse().toJavaList(), TransEnv.stat)));
        });
    }

    public static void main(String[] args) throws Exception {
        new MiniJavaParser(System.in);
        final var root = MiniJavaParser.Goal();

        final var env = new Lazy<TypeEnv>(z -> root.f1
                .accept(new ListVisitor<>(new ClassVisitor()), z)
                .fold(new T2<>(new Lazy<>(() -> 0), List.<Class>nul()),
                        (acc, mkClass) -> acc.consume((vtableOffset, classAcc) -> {
                            final var cls = mkClass.apply(vtableOffset);
                            return new T2<>(cls.nextVtableOffset, classAcc.cons(cls));
                        }))
                .consume((u, classes) -> new TypeEnv(List.nul(), classes, Optional.empty()))).get();

        final var funs = env.classes.flatMap(c -> c.methods.all
                .map(m -> m.translate(env.enterClass(c))))
                .cons(transMain(root.f0, env));

        final var prgm = new Program(funs.toJavaList());

        System.out.println(prgm.toString());
    }
}

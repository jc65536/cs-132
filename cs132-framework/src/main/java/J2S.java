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
        final var statSize = typeEnv.vtables.get()
                .fold(Optional.<Vtable>empty(), (mvtOpt, vt) -> mvtOpt
                        .filter(mvt -> mvt.offset > vt.offset)
                        .or(() -> Optional.of(vt)))
                .map(vt -> vt.offset + vt.size)
                .orElse(0);

        return new TransEnv(List.nul(), 0).genSym((tmp, transEnv) -> {
            final var statEnv = transEnv
                    .cons(new Move_Id_Integer(tmp, statSize))
                    .cons(new Alloc(TransEnv.stat, tmp));

            final var vtableEnv = typeEnv.vtables.get().fold(statEnv,
                    (acc, vt) -> vt.write(TransEnv.stat, tmp, acc));

            final var locals = main.f14.accept(new ListVisitor<>(new LocalVisitor()), typeEnv);

            final var newTypeEnv = typeEnv.addLocals(locals);

            final var bodyEnv = main.f15.nodes.stream().reduce(vtableEnv,
                    (acc, n) -> n.accept(new StmtVisitor(), new T2<>(newTypeEnv, acc)),
                    (u, v) -> v);

            return new FunctionDecl(new FunctionName("main"),
                    List.<Identifier>nul().toJavaList(),
                    new Block(bodyEnv.revCode.reverse().toJavaList(), TransEnv.stat));
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
                                return new T2<>(new Lazy<>(() -> vtableOffset.get() + cls.vtableSize.get()),
                                        classAcc.cons(cls));
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

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
        final var statSize = typeEnv.vtables.get().head().map(t -> t.b + t.a.size).orElse(0);

        final var t1 = new TransEnv(List.nul(), 0).genSym();
        final var tSym = t1.a;
        final var transEnv = t1.b;

        final var statEnv = transEnv.join(List.<Instruction>nul()
                .cons(new Alloc(TransEnv.statSym, tSym))
                .cons(new Move_Id_Integer(tSym, statSize)));

        final var vtableEnv = typeEnv.vtables.get().fold(statEnv, (acc, t) -> {
            final var vt = t.a;
            // System.out.println("Vtable: " + vt);
            return vt.write(TransEnv.statSym, tSym, acc);
        });

        final var locals = main.f14.accept(new ListVisitor<>(new LocalVisitor()), typeEnv);

        final var newTypeEnv = typeEnv.addLocals(locals);

        final var p = main.f15.accept(new FoldVisitor<>(new StmtVisitor(),
                (u, te) -> new T2<>(newTypeEnv, te)), new T2<>(newTypeEnv, vtableEnv));

        return new FunctionDecl(new FunctionName("main"),
                List.<Identifier>nul().toJavaList(),
                new Block(p.b.code.toJavaList(), TransEnv.statSym));
    }

    public static void main(String[] args) throws Exception {
        new MiniJavaParser(System.in);
        final var root = MiniJavaParser.Goal();

        final var env = new Lazy<TypeEnv>(z -> {
            final var classes = root.f1
                    .accept(new ListVisitor<>(new ClassVisitor()), z)
                    .fold(List.<Class>nul(), (classAcc, mkClass) -> classAcc.cons(mkClass.apply(classAcc)));
            return new TypeEnv(List.nul(), classes, Optional.empty());
        }).get();

        final var funs = env.classes.flatMap(c -> c.methods.get()
                .map(m -> m.translate(env.enterClass(c))))
                .cons(transMain(root.f0, env));

        final var prgm = new Program(funs.toJavaList());

        // System.out.println(env.classes.fold("", (s, c) -> String.format("%s\nVtables
        // of %s\n%s", s, c.name,
        // c.vtables.get().fold("", (t, v) -> t + v.toString() + "\n"))));

        System.out.println(prgm.toString());
    }
}

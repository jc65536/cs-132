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
        return new Trans(List.nul(), 0).applyTo(Trans.genSym(tmp -> tr -> {
            final var allocStat = tr
                    .cons(new Move_Id_Integer(tmp, typeEnv.statSize.get()))
                    .cons(new Alloc(Trans.stat, tmp));

            final var writeVtables = typeEnv.vtables.fold(allocStat,
                    (acc, vt) -> vt.write(tmp, acc));

            final var locals = main.f14.accept(new ListVisitor<>(new LocalVisitor()), typeEnv);
            final var localsEnv = typeEnv.addLocals(locals);
            final var trans = writeVtables.initLocals(locals);

            final var body = main.f15.accept(new ListVisitor<>(new StmtVisitor()), localsEnv)
                    .fold(trans, Trans::applyTo);

            return new FunctionDecl(new FunctionName("main"), java.util.List.of(),
                    new Block(body.codeRev.reverse().toJavaList(), Trans.stat));
        }));
    }

    public static void main(String[] args) throws Exception {
        new MiniJavaParser(System.in);
        final var root = MiniJavaParser.Goal();

        final var env = new Lazy<TypeEnv>(z -> root.f1
                .accept(new ListVisitor<>(new ClassVisitor()), z)
                .fold(new T2<>(List.<Class>nul(), new Lazy<>(() -> 0)),
                        (acc, mkClass) -> acc.consume(classAcc -> mkClass
                                .andThen(cls -> new T2<>(classAcc.cons(cls), cls.nextVtableOffset))))
                .consume(classes -> statSize -> new TypeEnv(List.nul(), classes, statSize, Optional.empty())))
                .get();

        final var funs = env.classes.flatMap(c -> c.methods.all
                .map(m -> m.translate(env.enterClass(c))))
                .cons(transMain(root.f0, env));

        final var prgm = new Program(funs.toJavaList());

        System.out.println(prgm.toString());
    }
}

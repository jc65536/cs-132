import java.util.*;

import cs132.IR.sparrow.*;
import cs132.IR.token.*;
import cs132.minijava.*;
import cs132.minijava.syntaxtree.MainClass;

public class J2S {
    static FunctionDecl transMain(MainClass main, TypeEnv env) {
        final var locals = main.f14.accept(new ListVisitor<>(new LocalVisitor()), env);
        final var newEnv = env.addLocals(locals);
        return new Trans(List.nul(), 0).applyTo(Trans.genSym(tmp -> tr -> tr
                .cons(new Move_Id_Integer(tmp, newEnv.statSize.get()))
                .cons(new Alloc(Trans.stat, tmp))
                .applyTo(tr1 -> newEnv.vtables.fold(tr1, (acc, vt) -> acc.applyTo(vt.write(tmp)))))
                .andThen(Trans.initLocals(locals))
                .andThen(tr -> main.f15.accept(new ListVisitor<>(new StmtVisitor()), newEnv)
                        .fold(tr, Trans::applyTo))
                .andThen(tr -> new Block(tr.reverse().toJavaList(), Trans.stat))
                .andThen(block -> new FunctionDecl(new FunctionName("main"), java.util.List.of(), block)));
    }

    public static void main(String[] args) throws Exception {
        new MiniJavaParser(System.in);
        final var root = MiniJavaParser.Goal();

        new Lazy<TypeEnv>(z -> root.f1.accept(new ListVisitor<>(new ClassVisitor()), z)
                .fold(new T2<>(List.<Class>nul(), new Lazy<>(() -> 0)), (acc, mkClass) -> acc
                        .then(classes -> mkClass.andThen(cls -> new T2<>(classes.cons(cls), cls.vtableEnd))))
                .then(classes -> statSize -> new TypeEnv(List.nul(), classes, statSize, Optional.empty())))
                .then(env -> env.classes.flatMap(c -> c.methods.all
                        .map(m -> m.translate(env.enterClass(c))))
                        .cons(transMain(root.f0, env)))
                .then(fns -> new Program(fns.toJavaList()))
                .then(prgm -> {
                    System.out.println(prgm);
                    return 0;
                })
                .get();
    }
}

import java.util.*;

import cs132.IR.sparrow.Alloc;
import cs132.IR.sparrow.Block;
import cs132.IR.sparrow.FunctionDecl;
import cs132.IR.sparrow.Instruction;
import cs132.IR.sparrow.Move_Id_Integer;
import cs132.IR.sparrow.Program;
import cs132.IR.token.FunctionName;
import cs132.IR.token.Identifier;
import cs132.minijava.*;

public class J2S {
    static final boolean DEBUG = true;

    static <T> Optional<T> error(String s) {
        System.out.println(DEBUG ? s : "Type error");
        System.exit(1);
        return Optional.empty();
    }

    static Instruction comment(String s) {
        return new Move_Id_Integer(new Identifier("# " + s), 0);
    }

    static FunctionDecl transMain(TransEnv argu) {
        final var statSize = argu.vtables.get().head().map(t -> t.b + t.a.size).orElse(0);

        final var t1 = argu.genSym();
        final var tSym = t1.a;
        final var t2 = t1.b.genSym();
        final var stat = t2.a;
        final var env = t2.b;

        final var statEnv = env.join(List.<Instruction>nul()
                .cons(new Alloc(stat, tSym))
                .cons(new Move_Id_Integer(tSym, statSize)));

        final var t3 = statEnv.vtables.get().fold(statEnv, (acc, t) -> {
            final var vt = t.a;
            System.out.println("Vtable: " + vt);
            return vt.write(stat, tSym, acc);
        });
        final var zEnv = t3;

        return new FunctionDecl(new FunctionName("main"), List.<Identifier>nul().toJavaList(),
                new Block(zEnv.code.toJavaList(), new Identifier("ret")));
    }

    public static void main(String[] args) throws Exception {
        new MiniJavaParser(System.in);
        final var root = MiniJavaParser.Goal();

        final var mainName = root.f0.f1.f0.tokenImage;

        final var env = new Lazy<TransEnv>(z -> {
            final var classes = root.f1
                    .accept(new ListVisitor<>(new ClassVisitor()), z)
                    .fold(List.<Class>nul(), (classAcc, mkClass) -> classAcc.cons(mkClass.apply(classAcc)));
            return new TransEnv(new TypeEnv(List.nul(), classes, Optional.empty()), List.nul(), 0);
        }).get();

        final var funs = env.classes.flatMap(c -> c.methods.get())
                .map(Method::translate)
                .cons(transMain(env));

        final var prgm = new Program(funs.toJavaList());

        System.out.println(env.classes.fold("", (s, c) -> String.format("%s\nVtables of %s\n%s", s, c.name,
                c.vtables.get().fold("", (t, v) -> t + v.toString() + "\n"))));

        System.out.println(prgm.toString());
    }
}

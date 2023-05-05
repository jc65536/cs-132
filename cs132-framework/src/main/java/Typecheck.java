import java.util.*;

import cs132.minijava.MiniJavaParser;
import cs132.minijava.syntaxtree.*;

public class Typecheck {
    static final boolean DEBUG = false;

    static <T> Optional<T> error(String s) {
        System.out.println(DEBUG ? s : "Type error");
        System.exit(1);
        return Optional.empty();
    }

    static boolean typecheck(MainClass n, TypeEnv argu) {
        final var argsName = n.f11.f0.tokenImage;
        return n.f14.accept(new ListVisitor<>(new SymPairVisitor()), argu)
                .map(var -> Optional.of(var).filter(u -> !var.name.equals(argsName)))
                .foldFalliable(List.<SymPair>nul(), Named::distinct)
                .or(() -> error("Duplicate locals"))
                .map(vars -> new TypeEnv(vars, argu.classes, Optional.empty()))
                .map(env -> n.f15.accept(new ListVisitor<>(new StmtVisitor()), env).forAll(b -> b))
                .orElse(false);
    }

    static boolean typecheck(Method m, TypeEnv argu) {
        final var localNodes = m.body.f7;
        final var retExpr = m.body.f10;
        final var stmtNodes = m.body.f8;
        return localNodes.accept(new ListVisitor<>(new SymPairVisitor()), argu)
                .foldFalliable(m.params, Named::distinct)
                .or(() -> error("Duplicate locals"))
                .map(vars -> new TypeEnv(vars, argu.classes, argu.currClass))
                .filter(env -> checkExpr(retExpr, m.retType, env))
                .map(env -> stmtNodes.accept(new ListVisitor<>(new StmtVisitor()), env).forAll(b -> b))
                .orElse(false);
    }

    public static void main(String[] args) throws Exception {
        new MiniJavaParser(System.in);
        final var root = MiniJavaParser.Goal();
        final var mainName = root.f0.f1.f0.tokenImage;

        final var env = new Lazy<TypeEnv>(z -> root.f1
                .accept(new ListVisitor<>(new ClassVisitor()), z)
                .map(c -> Optional.of(c).filter(u -> !c.name.equals(mainName)))
                .foldFalliable(List.<Class>nul(), Named::distinct)
                .or(() -> error("Duplicate classes"))
                .map(classes -> new TypeEnv(List.nul(), classes, Optional.empty()))
                .get()).get();

        Optional.of(0)
                .filter(u -> env.classes.forAll(c -> c.acyclic(List.nul())))
                .or(() -> error("Cyclic class extension"))
                .filter(u -> typecheck(root.f0, env))
                .filter(u -> env.classes.forAll(c -> c.methods.get()
                        .forAll(m -> typecheck(m, env.enterClass(c)))))
                .or(() -> error("Unknown error"))
                .ifPresent(u -> System.out.println("Program type checked successfully"));
    }

    static boolean checkExpr(Node n, Type expected, TypeEnv argu) {
        return n.accept(new ExprVisitor(), argu)
                .map(t -> t.subtypes(expected))
                .orElse(false);
    }
}

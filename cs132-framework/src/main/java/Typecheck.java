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

    static boolean typecheck(MainClass mainNode, TypeEnv argu) {
        final var argsName = mainNode.f11.f0.tokenImage;
        final var stmtNodes = mainNode.f15.nodes;
        return mainNode.f14.accept(new ListVisitor<>(new SymPairVisitor()), argu)
                .mapFalliable(var -> Optional.of(var).filter(u -> !var.name.equals(argsName)))
                .flatMap(vars -> vars.forceDistinct(Named::distinct))
                .or(() -> Typecheck.error("Duplicate locals"))
                .map(vars -> stmtNodes.stream().allMatch(n -> n.accept(new StmtVisitor(), argu.addLocals(vars))))
                .orElse(false);
    }

    static boolean typecheck(Method m, TypeEnv argu) {
        final var localNodes = m.body.f7;
        final var retExpr = m.body.f10;
        final var stmtNodes = m.body.f8.nodes;
        return localNodes.accept(new ListVisitor<>(new SymPairVisitor()), argu)
                .mapFalliable(var -> Optional.of(var).filter(u -> Named.distinct(argu.locals, var)))
                .flatMap(vars -> vars.forceDistinct(Named::distinct))
                .or(() -> Typecheck.error("Duplicate locals"))
                .map(argu::addLocals)
                .map(env -> Typecheck.checkExpr(retExpr, m.retType, env)
                        && stmtNodes.stream().allMatch(n -> n.accept(new StmtVisitor(), env)))
                .orElse(false);
    }

    public static void main(String[] args) throws Exception {
        new MiniJavaParser(System.in);
        final var root = MiniJavaParser.Goal();
        final var mainName = root.f0.f1.f0.tokenImage;

        final var env = new Lazy<TypeEnv>(z -> root.f1
                .accept(new ListVisitor<>(new ClassVisitor()), z)
                .forceDistinct((cs, c) -> !c.name.equals(mainName) && Named.distinct(cs, c))
                .or(() -> Typecheck.error("Duplicate classes"))
                .map(cs -> new TypeEnv(List.nul(), cs, Optional.empty()))
                .get()).get();

        Optional.of(0)
                .filter(u -> env.classes.forAll(c -> c.acyclic(List.nul())))
                .or(() -> Typecheck.error("Cyclic class extension"))
                .filter(u -> typecheck(root.f0, env) && env.classes.forAll(c -> c.methods.get()
                        .forAll(m -> typecheck(m, env.enterClassMethod(c, m)))))
                .or(() -> Typecheck.error("Unknown error"))
                .ifPresent(u -> System.out.println("Program type checked successfully"));
    }

    static boolean checkExpr(Node n, Type expected, TypeEnv argu) {
        return n.accept(new ExprVisitor(), argu)
                .map(t -> t.subtypes(expected))
                .orElse(false);
    }
}

import java.util.*;

import cs132.minijava.MiniJavaParser;
import cs132.minijava.syntaxtree.*;

public class Typecheck {
    static final boolean DEBUG = false;

    static <T> Optional<T> error(String s) {
        if (DEBUG)
            System.out.println(s);
        else
            System.out.println("Type error");

        System.exit(1);
        return Optional.empty();
    }

    static boolean typecheck(MainClass n, TypeEnv argu) {
        final var argsName = n.f11.f0.tokenImage;
        final var stmtNodes = n.f15.nodes;
        return n.f14.accept(new ListVisitor<>(new SymPairVisitor()), argu)
                .forceDistinct((locals, var) -> !var.name().equals(argsName) && Named.distinct(locals, var))
                .or(() -> Typecheck.error("Duplicate locals"))
                .map(locals -> argu.addLocals(locals))
                .map(env -> stmtNodes.stream().allMatch(node -> node.accept(new StmtVisitor(), env)))
                .orElse(false);
    }

    static boolean typecheck(Method m, TypeEnv argu) {
        final var localNodes = m.body.f7;
        final var retExpr = m.body.f10;
        final var stmtNodes = m.body.f8.nodes;
        return localNodes.accept(new ListVisitor<>(new SymPairVisitor()), argu)
                .forceDistinct((locals, var) -> Named.distinct(argu.locals.join(locals), var))
                .or(() -> Typecheck.error("Duplicate locals"))
                .map(locals -> argu.addLocals(locals))
                .map(env -> Typecheck.checkExpr(retExpr, m.retType, env)
                        && stmtNodes.stream().allMatch(node -> node.accept(new StmtVisitor(), env)))
                .orElse(false);
    }

    public static void main(String[] args) throws Exception {
        new MiniJavaParser(System.in);
        final var root = MiniJavaParser.Goal();
        final var mainClassName = root.f0.f1.f0.tokenImage;

        final var env = new Lazy<TypeEnv>(z -> root.f1.accept(new ListVisitor<>(new ClassVisitor()), z)
                .forceDistinct((classes, c) -> !c.name().equals(mainClassName) && Named.distinct(classes, c))
                .or(() -> Typecheck.error("Duplicate classes"))
                .map(classes -> new TypeEnv(List.nul(), classes, Optional.empty()))
                .get()).get();

        final var acyclic = env.classes.forAll(c -> c.acyclic(List.nul()));

        if (!acyclic)
            Typecheck.error("Cyclic class extension");

        if (Typecheck.DEBUG)
            System.out.println(env);

        final var typechecks = acyclic
                && typecheck(root.f0, env)
                && env.classes.forAll(c -> c.body().methods.forAll(m -> typecheck(m, env.enterClassMethod(c, m))));

        if (typechecks)
            System.out.println("Program type checked successfully");
        else
            Typecheck.error("Type error?");
    }

    static boolean checkExpr(Node node, Type expected, TypeEnv argu) {
        return node.accept(new ExprVisitor(), argu)
                .map(type -> type.subtypes(expected))
                .orElse(false);
    }
}

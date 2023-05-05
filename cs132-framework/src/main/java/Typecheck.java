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

    static <T> Optional<T> condOpt(T val, boolean b) {
        return Optional.of(val).filter(u -> b);
    }

    static boolean typecheck(MainClass n, TypeEnv argu) {
        final var argsName = n.f11.f0.tokenImage;
        return n.f14.accept(new ListVisitor<>(new SymPairVisitor()), argu)
                .mapFalliable(var -> condOpt(var, !var.name.equals(argsName)))
                .flatMap(vars -> vars.forceDistinct(Named::distinct))
                .or(() -> Typecheck.error("Duplicate locals"))
                .map(argu::addLocals)
                .map(env -> n.f15.accept(new ListVisitor<>(new StmtVisitor()), env).forAll(b -> b))
                .orElse(false);
    }

    static boolean typecheck(Method m, TypeEnv argu) {
        final var localNodes = m.body.f7;
        final var retExpr = m.body.f10;
        final var stmtNodes = m.body.f8;
        return localNodes.accept(new ListVisitor<>(new SymPairVisitor()), argu)
                .mapFalliable(var -> condOpt(var, Named.distinct(argu.locals, var)))
                .flatMap(vars -> vars.forceDistinct(Named::distinct))
                .or(() -> Typecheck.error("Duplicate locals"))
                .map(argu::addLocals)
                .filter(env -> Typecheck.checkExpr(retExpr, m.retType, env))
                .map(env -> stmtNodes.accept(new ListVisitor<>(new StmtVisitor()), env).forAll(b -> b))
                .orElse(false);
    }

    public static void main(String[] args) throws Exception {
        new MiniJavaParser(System.in);
        final var root = MiniJavaParser.Goal();
        final var mainName = root.f0.f1.f0.tokenImage;

        final var env = new Lazy<TypeEnv>(z -> root.f1
                .accept(new ListVisitor<>(new ClassVisitor()), z)
                .mapFalliable(c -> condOpt(c, !c.name.equals(mainName)))
                .flatMap(classes -> classes.forceDistinct(Named::distinct))
                .or(() -> Typecheck.error("Duplicate classes"))
                .map(cs -> new TypeEnv(List.nul(), cs, Optional.empty()))
                .get()).get();

        Optional.of(0)
                .filter(u -> env.classes.forAll(c -> c.acyclic(List.nul())))
                .or(() -> Typecheck.error("Cyclic class extension"))
                .filter(u -> typecheck(root.f0, env))
                .filter(u -> env.classes.forAll(c -> c.methods.get()
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

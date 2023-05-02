import java.util.*;

import cs132.minijava.MiniJavaParser;
import cs132.minijava.syntaxtree.*;

public class Typecheck {
    static boolean typecheck(MainClass n, TypeEnv argu) {
        final var argsName = n.f11.f0.tokenImage;

        return n.f14.accept(new ListVisitor<>(new SymPairVisitor()), argu)
                .forceDistinct((locals, pair) -> !pair.name().equals(argsName) && Named.distinct(locals, pair))
                .or(() -> Util.error("Duplicate locals"))
                .map(locals -> argu.addLocals(locals))
                .filter(typeEnv -> n.f15.nodes.stream().allMatch(node -> node.accept(new StmtVisitor(), typeEnv)))
                .isPresent();
    }

    static boolean typecheck(Method m, TypeEnv argu) {
        return m.body.f7.accept(new ListVisitor<>(new SymPairVisitor()), argu)
                .forceDistinct((locals, pair) -> Named.distinct(argu.locals, pair) && Named.distinct(locals, pair))
                .or(() -> Util.error("Duplicate locals"))
                .map(locals -> argu.addLocals(locals))
                .filter(typeEnv -> Util.checkExpr(m.body.f10, m.retType, typeEnv))
                .filter(typeEnv -> m.body.f8.nodes.stream().allMatch(node -> node.accept(new StmtVisitor(), typeEnv)))
                .isPresent();
    }

    public static void main(String[] args) throws Exception {
        final var root = new MiniJavaParser(System.in).Goal();

        final var mainClassName = root.f0.f1.f0.tokenImage;

        final var typeEnv = new Lazy<TypeEnv>(z -> root.f1.accept(new ListVisitor<>(new ClassVisitor()), z)
                .forceDistinct((classes, c) -> !c.name().equals(mainClassName) && Named.distinct(classes, c))
                .or(() -> Util.error("Duplicate classes"))
                .map(classes -> new TypeEnv(List.nul(), classes, Optional.empty()))
                .get()).get();

        final var acyclic = typeEnv.classes.forAll(c -> c.acyclic(List.nul()));

        if (!acyclic)
            Util.error("Cyclic class extension");

        if (Util.DEBUG)
            System.out.println(typeEnv);

        final var typechecks = acyclic
                && typecheck(root.f0, typeEnv)
                && typeEnv.classes.forAll(c -> c.body().methods
                        .forAll(m -> typecheck(m, typeEnv.enterClassMethod(c, m))));

        if (typechecks)
            System.out.println("Program type checked successfully");
        else
            Util.error("Type error?");
    }
}

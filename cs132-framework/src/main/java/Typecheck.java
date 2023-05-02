import java.util.*;

import cs132.minijava.MiniJavaParser;
import cs132.minijava.syntaxtree.*;

public class Typecheck {
    static boolean typecheck(MainClass n, TypeEnv argu) {
        final var argsSym = n.f11.f0.tokenImage;

        final var localVisitor = new ListVisitor<SymPair, SymPair, TypeEnv>(new SymPairVisitor(),
                (locals, pair) -> Util.distinct(locals, pair).filter(u -> !pair.name().equals(argsSym)));

        return n.f14.accept(localVisitor, argu)
                .or(() -> Util.error("Duplicate locals"))
                .map(locals -> argu.addLocals(locals))
                .filter(typeEnv -> n.f15.nodes.stream().allMatch(node -> node.accept(new StmtVisitor(), typeEnv)))
                .isPresent();
    }

    static boolean typecheck(Method m, TypeEnv argu) {
        final var localVisitor = new ListVisitor<SymPair, SymPair, TypeEnv>(new SymPairVisitor(),
                (locals, pair) -> Util.distinct(locals, pair).flatMap(u -> Util.distinct(argu.locals, pair)));

        return m.body.f7.accept(localVisitor, argu)
                .or(() -> Util.error("Duplicate locals"))
                .map(locals -> argu.addLocals(locals))
                .filter(typeEnv -> Util.checkExpr(m.body.f10, m.retType, typeEnv))
                .filter(typeEnv -> m.body.f8.nodes.stream().allMatch(node -> node.accept(new StmtVisitor(), typeEnv)))
                .isPresent();
    }

    public static void main(String[] args) throws Exception {
        final var root = new MiniJavaParser(System.in).Goal();

        final var mainClassName = root.f0.f1.f0.tokenImage;

        final var classVisitor = new ListVisitor<Class, Class, Lazy<TypeEnv>>(new ClassVisitor(),
                (classes, clas) -> Util.distinct(classes, clas).filter(u -> !clas.name().equals(mainClassName)));

        final var typeEnv = new Lazy<TypeEnv>(z -> root.f1.accept(classVisitor, z)
                .or(() -> Util.error("Duplicate classes"))
                .map(classes -> new TypeEnv(List.nul(), classes, Optional.empty()))
                .get()).get();

        // System.out.println(typeEnv);

        final var typechecks = typecheck(root.f0, typeEnv)
                && typeEnv.classes.forAll(c -> c.acyclic(c) && c.body().methods
                        .forAll(m -> typecheck(m, typeEnv.enterClassMethod(c, m))));

        if (typechecks)
            System.out.println("Program type checked successfully");
        else
            Util.error("Type error?");
    }
}

import java.util.*;

import cs132.minijava.MiniJavaParser;
import cs132.minijava.syntaxtree.*;

public class Typecheck {
    static boolean typecheck(MainClass n, TypeEnv argu) {
        final var argsSym = n.f11.f0.tokenImage;

        final var localVisitor = new ListVisitor<>(new SymPairVisitor(),
                (locals, pair) -> !pair.name().equals(argsSym) && Named.distinct(locals, pair),
                "Duplicate locals");

        final var locals = n.f14.accept(localVisitor, argu);

        final var typeEnv = argu.addLocals(locals);
        final var stmtNodes = n.f15.nodes;

        return stmtNodes.stream().allMatch(node -> node.accept(new StmtVisitor(), typeEnv));
    }

    static boolean typecheck(Method m, TypeEnv argu) {
        final var localVisitor = new ListVisitor<>(new SymPairVisitor(),
                (locals, pair) -> Named.distinct(argu.locals, pair) && Named.distinct(locals, pair),
                "Duplicate locals");

        final var locals = m.body.f7.accept(localVisitor, argu);

        final TypeEnv typeEnv = argu.addLocals(locals);
        final var stmtNodes = m.body.f8.nodes;
        final var retType = m.retType;

        return Util.expect(Util.checkExpr(m.body.f10, retType, typeEnv), "Return type error")
                && stmtNodes.stream().allMatch(node -> node.accept(new StmtVisitor(), typeEnv));
    }

    public static void main(String[] args) throws Exception {
        final var root = new MiniJavaParser(System.in).Goal();

        final var mainClassName = root.f0.f1.f0.tokenImage;

        final var classVisitor = new ListVisitor<>(new ClassVisitor(),
                (classes, clas) -> !clas.name().equals(mainClassName) && Named.distinct(classes, clas),
                "Duplicate class");

        final var typeEnv = new Lazy<TypeEnv>(z -> new TypeEnv(List.nul(),
                root.f1.accept(classVisitor, z),
                Optional.empty()))
                .get();

        // System.out.println(typeEnv);

        final var typechecks = typecheck(root.f0, typeEnv)
                && typeEnv.classes.forAll(c -> c.acyclic(c) && c.body().methods
                        .forAll(m -> typecheck(m, typeEnv.enterClassMethod(c, m))));

        if (typechecks)
            System.out.println("Program type checked successfully");
        else
            Util.error("Type error");
    }
}

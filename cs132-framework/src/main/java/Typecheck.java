import java.util.*;

import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;
import cs132.minijava.MiniJavaParser;

class TypeDeclVisitor extends GJNoArguDepthFirst<TypeEnv> {
    @Override
    public TypeEnv visit(Goal n) {
        final var mainClassName = n.f0.f1.f0.tokenImage;

        final var classVisitor = new ListVisitor<>(new ClassVisitor(),
                (classes, clas) -> !(clas.name.equals(mainClassName) || classes.exists(c -> c.name.equals(clas.name))));

        return new Lazy<TypeEnv>(z -> {
            final var classList = n.f1.accept(classVisitor, z);
            return new TypeEnv(List.nul(), classList, Optional.empty(), Optional.empty());
        }).get();
    }
}

public class Typecheck {
    public static void main(String[] args) throws Exception {
        final var root = new MiniJavaParser(System.in).Goal();

        final var typeEnv = root.accept(new TypeDeclVisitor());

        // System.out.println(typeEnv);

        final var mainClassTypeChecks = root.f0.accept(new TypecheckVisitor(), typeEnv);

        final var typeChecks = mainClassTypeChecks && typeEnv.classList.forAll(c -> c.get().methods
                .forAll(m -> m.body.accept(new TypecheckVisitor(), typeEnv.enterClassMethod(c, m))));

        if (typeChecks)
            System.out.println("Program type checked successfully");
        else
            Util.error("Typecheck failed");
    }
}

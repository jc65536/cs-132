import java.util.*;
import java.util.function.*;

import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;
import cs132.minijava.MiniJavaParser;

class Util {
    static <T> T error(String s) {
        System.out.println("Type error");
        System.exit(1);
        return null;
    }

    static boolean expect(boolean b, String msg) {
        return b || Util.<Boolean>error(msg);
    }
}

class TypeDeclVisitor extends GJNoArguDepthFirst<TypeEnv> {
    @Override
    public TypeEnv visit(Goal n) {
        final var mainClassName = n.f0.f1.f0.tokenImage;

        final var classVisitor = new ListVisitor<>(new ClassVisitor(),
                (classes, clas) -> !clas.name().equals(mainClassName) && Named.distinct(classes, clas),
                "Duplicate class");

        return new Lazy<TypeEnv>(z -> new TypeEnv(List.nul(),
                n.f1.accept(classVisitor, z),
                Optional.empty(),
                Optional.empty()))
                .get();
    }
}

public class Typecheck {
    public static void main(String[] args) throws Exception {
        final var root = new MiniJavaParser(System.in).Goal();

        final var typeEnv = root.accept(new TypeDeclVisitor());

        // System.out.println(typeEnv);

        final var mainTypechecks = root.f0.accept(new TypecheckVisitor(), typeEnv);

        final var typechecks = mainTypechecks
                && typeEnv.classList.forAll(c -> c.body().methods
                        .forAll(m -> m.body.accept(new TypecheckVisitor(), typeEnv.enterClassMethod(c, m))));

        if (typechecks)
            System.out.println("Program type checked successfully");
        else
            Util.error("Typecheck failed");
    }
}

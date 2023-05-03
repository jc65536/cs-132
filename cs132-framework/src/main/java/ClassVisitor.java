import java.util.*;

import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

class MethodVisitor extends GJDepthFirst<Method, TypeEnv> {
    @Override
    public Method visit(MethodDeclaration n, TypeEnv argu) {
        return n.f4.accept(new ListVisitor<>(new SymPairVisitor()), argu)
                .forceDistinct(Named::distinct)
                .or(() -> Typecheck.error("Duplicate params"))
                .map(params -> {
                    final var name = n.f2.f0.tokenImage;
                    final var retType = n.f1.accept(new TypeVisitor(), argu);
                    return new Method(name, params, retType, n);
                })
                .get();
    }
}

public class ClassVisitor extends GJDepthFirst<Class, Lazy<TypeEnv>> {
    @Override
    public Class visit(TypeDeclaration n, Lazy<TypeEnv> argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Class visit(ClassDeclaration n, Lazy<TypeEnv> argu) {
        final var className = n.f1.f0.tokenImage;
        return new Class(className,
                Optional.empty(),
                (c) -> mkClassBody(c, n.f3, n.f4, argu.get()));
    }

    @Override
    public Class visit(ClassExtendsDeclaration n, Lazy<TypeEnv> argu) {
        final var className = n.f1.f0.tokenImage;
        final var superName = n.f3.f0.tokenImage;
        return new Class(className,
                Optional.of(() -> argu.get().classLookup(superName)),
                (c) -> mkClassBody(c, n.f5, n.f6, argu.get()));
    }

    static boolean noOverloading(Class c, Method method) {
        return c.superClass()
                .map(sc -> sc.body().methods.forAll(m -> !m.name().equals(method.name()) || m.typeEquals(method))
                        && noOverloading(sc, method))
                .orElse(true);
    }

    static ClassBody mkClassBody(Class c, NodeListOptional fieldNodes, NodeListOptional methodNodes, TypeEnv argu) {
        return fieldNodes.accept(new ListVisitor<>(new SymPairVisitor()), argu)
                .forceDistinct(Named::distinct)
                .or(() -> Typecheck.error("Duplicate fields"))
                .flatMap(fields -> methodNodes.accept(new ListVisitor<>(new MethodVisitor()), argu)
                        .forceDistinct((methods, m) -> Named.distinct(methods, m) && noOverloading(c, m))
                        .or(() -> Typecheck.error("Duplicate methods"))
                        .map(methods -> new ClassBody(fields, methods)))
                .get();

    }
}

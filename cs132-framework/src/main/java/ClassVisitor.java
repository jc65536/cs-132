import java.util.*;

import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

class MethodVisitor extends GJDepthFirst<Method, TypeEnv> {
    @Override
    public Method visit(MethodDeclaration n, TypeEnv argu) {
        final var name = n.f2.f0.tokenImage;
        final var paramVisitor = new ListVisitor<>(new SymPairVisitor(), Named::distinct, "Duplicate params");
        final var params = n.f4.accept(paramVisitor, argu);
        final var retType = n.f1.accept(new TypeVisitor(), argu);
        return new Method(name, params, retType, n);
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
        return new Class(className, (c) -> mkClassBody(n.f3, n.f4, Optional.empty(), argu.get()));
    }

    @Override
    public Class visit(ClassExtendsDeclaration n, Lazy<TypeEnv> argu) {
        final var className = n.f1.f0.tokenImage;
        final var superName = n.f3.f0.tokenImage;
        return new Class(className, (c) -> {
            final var typeEnv = argu.get();
            final var superClass = typeEnv.classLookup(superName);

            if (superClass.acyclic(c))
                return mkClassBody(n.f5, n.f6, Optional.of(superClass), typeEnv);
            else
                return Util.error("Cyclic class extension");
        });
    }

    static boolean noOverloading(Optional<Class> superClass, Method method) {
        return superClass
                .map(sc -> sc.body().methods
                        .forAll(m -> !m.name().equals(method.name()) || m.typeEquals(method))
                        && noOverloading(sc.body().superClass, method))
                .orElse(true);
    }

    static ClassBody mkClassBody(NodeListOptional fieldNodes, NodeListOptional methodNodes, Optional<Class> sc,
            TypeEnv argu) {

        final var fieldVisitor = new ListVisitor<>(new SymPairVisitor(), Named::distinct, "Duplicate fields");
        final var fields = fieldNodes.accept(fieldVisitor, argu);

        final var methodVisitor = new ListVisitor<>(new MethodVisitor(),
                (methods, method) -> Named.distinct(methods, method) && noOverloading(sc, method),
                "Duplicate methods");

        final var methods = methodNodes.accept(methodVisitor, argu);

        return new ClassBody(fields, methods, sc);
    }
}

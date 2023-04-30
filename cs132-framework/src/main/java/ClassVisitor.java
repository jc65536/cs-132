import java.util.*;

import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

class MethodVisitor extends GJDepthFirst<Method, TypeEnv> {
    @Override
    public Method visit(MethodDeclaration n, TypeEnv argu) {
        final var name = n.f2.f0.tokenImage;

        final var paramVisitor = new ListVisitor<>(new SymPairVisitor(),
                (params, param) -> !params.exists(p -> p.sym.equals(param.sym)),
                "Duplicate params");

        final var params = n.f4.accept(paramVisitor, argu);
        final var retType = n.f1.accept(new TypeVisitor(), argu);

        return new Method(name, params, retType, n);
    }
}

public class ClassVisitor extends GJDepthFirst<Class, Lazy<TypeEnv>> {
    @Override
    public Class visit(TypeDeclaration n, Lazy<TypeEnv> argu) {
        return n.f0.accept(this, argu);
    }

    @Override
    public Class visit(ClassDeclaration n, Lazy<TypeEnv> argu) {
        final var className = n.f1.f0.tokenImage;
        return new Class(className, () -> mkClassBody(n.f3, n.f4, Optional.empty(), argu.get()));
    }

    @Override
    public Class visit(ClassExtendsDeclaration n, Lazy<TypeEnv> argu) {
        final var className = n.f1.f0.tokenImage;
        final var superName = n.f3.f0.tokenImage;
        return new Class(className, () -> {
            final var superClass = argu.get().classLookup(superName);
            return mkClassBody(n.f5, n.f6, Optional.of(superClass), argu.get());
        });
    }

    static boolean noOverloading(Optional<Class> superClass, Method method) {
        return superClass
                .map(c -> c.get().methods.forAll(m -> !m.name.equals(method.name) || m.typeEquals(method))
                        && noOverloading(c.get().superClass, method))
                .orElse(true);
    }

    static ClassBody mkClassBody(NodeListOptional fieldNodes, NodeListOptional methodNodes, Optional<Class> superClass,
            TypeEnv argu) {

        final var fieldVisitor = new ListVisitor<>(new SymPairVisitor(),
                (fields, field) -> !fields.exists(f -> f.sym.equals(field.sym)),
                "Duplicate fields");

        final var fields = fieldNodes.accept(fieldVisitor, argu);

        final var methodVisitor = new ListVisitor<>(new MethodVisitor(),
                (methods, method) -> !methods.exists(m -> m.name.equals(method.name))
                        && noOverloading(superClass, method),
                "Duplicate methods");

        final var methods = methodNodes.accept(methodVisitor, argu);

        return new ClassBody(fields, methods, superClass);
    }
}

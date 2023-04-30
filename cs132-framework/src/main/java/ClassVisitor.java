import java.util.*;

import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

class ClassVisitor extends GJDepthFirst<Class, Lazy<TypeEnv>> {
    @Override
    public Class visit(TypeDeclaration n, Lazy<TypeEnv> argu) {
        return n.f0.accept(this, argu);
    }

    @Override
    public Class visit(ClassDeclaration n, Lazy<TypeEnv> argu) {
        final var className = n.f1.f0.tokenImage;
        final var fieldNodes = n.f3.nodes;
        final var methodNodes = n.f4.nodes;
        return new Class(className, () -> mkClassBody(fieldNodes, methodNodes, Optional.empty(), argu.get()));
    }

    @Override
    public Class visit(ClassExtendsDeclaration n, Lazy<TypeEnv> argu) {
        final var className = n.f1.f0.tokenImage;
        final var superName = n.f3.f0.tokenImage;
        final var fieldNodes = n.f5.nodes;
        final var methodNodes = n.f6.nodes;
        return new Class(className, () -> {
            final var superClass = argu.get().classLookup(superName);
            return mkClassBody(fieldNodes, methodNodes, Optional.of(superClass), argu.get());
        });
    }

    static ClassBody mkClassBody(Vector<Node> fieldNodes, Vector<Node> methodNodes, Optional<Class> superClass,
            TypeEnv argu) {

        final List<SymPair> ownFields = fieldNodes.stream().reduce(List.nul(),
                (fields, node) -> {
                    final var field = node.accept(new SymPairVisitor(), argu);

                    if (fields.exists(f -> f.sym.equals(field.sym)))
                        return Util.error("Duplicate field name");
                    else
                        return fields.cons(field);
                },
                (u, v) -> v);

        final var superMethods = superClass.map(c -> c.get().methods).orElse(List.nul());

        final List<Method> ownMethods = methodNodes.stream().reduce(List.nul(),
                (methods, node) -> {
                    final var method = node.accept(new MethodVisitor(), argu);

                    if (methods.exists(m -> m.name.equals(method.name)))
                        Util.error("Duplicate method name");

                    if (superMethods.exists(m -> m.name.equals(method.name) && !m.typeEquals(method)))
                        Util.error("Overloading method in superclass");

                    return methods.cons(method);
                },
                (u, v) -> v);

        return new ClassBody(ownFields, ownMethods, superClass);
    }
}

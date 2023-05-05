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
                () -> mkFields(n.f3, argu.get()),
                (c) -> mkMethods(c, n.f4, argu.get()));
    }

    @Override
    public Class visit(ClassExtendsDeclaration n, Lazy<TypeEnv> argu) {
        final var className = n.f1.f0.tokenImage;
        final var superName = n.f3.f0.tokenImage;
        return new Class(className,
                Optional.of(() -> argu.get().classLookup(superName)),
                () -> mkFields(n.f5, argu.get()),
                (c) -> mkMethods(c, n.f6, argu.get()));
    }

    static List<SymPair> mkFields(NodeListOptional fieldNodes, TypeEnv argu) {
        return fieldNodes.accept(new ListVisitor<>(new SymPairVisitor()), argu)
                .forceDistinct(Named::distinct)
                .or(() -> Typecheck.error("Duplicate fields"))
                .get();
    }

    static List<Method> mkMethods(Class c, NodeListOptional methodNodes, TypeEnv argu) {
        return methodNodes.accept(new ListVisitor<>(new MethodVisitor()), argu)
                .mapFalliable(c::noOverloading)
                .flatMap(methods -> methods.forceDistinct(Named::distinct))
                .or(() -> Typecheck.error("Duplicate methods"))
                .get();
    }
}

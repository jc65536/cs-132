import java.util.*;

import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

class MethodVisitor extends GJDepthFirst<Method, TypeEnv> {
    @Override
    public Method visit(MethodDeclaration n, TypeEnv argu) {
        final var name = n.f2.f0.tokenImage;

        final var paramVisitor = new ListVisitor<SymPair, SymPair, TypeEnv>(new SymPairVisitor(),
                (params, param) -> Util.condOpt(param, Named.distinct(params, param)));

        final var params = n.f4.accept(paramVisitor, argu)
                .or(() -> Util.error("Duplicate params"))
                .get();

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
        final var fieldVisitor = new ListVisitor<SymPair, SymPair, TypeEnv>(new SymPairVisitor(),
                (fields, field) -> Util.condOpt(field, Named.distinct(fields, field)));

        final var fields = fieldNodes.accept(fieldVisitor, argu)
                .or(() -> Util.error("Duplicate fields"))
                .get();

        final var methodVisitor = new ListVisitor<Method, Method, TypeEnv>(new MethodVisitor(),
                (methods, method) -> Util.condOpt(method, Named.distinct(methods, method) && noOverloading(c, method)));

        final var methods = methodNodes.accept(methodVisitor, argu)
                .or(() -> Util.error("Duplicate methods"))
                .get();

        return new ClassBody(fields, methods);
    }
}

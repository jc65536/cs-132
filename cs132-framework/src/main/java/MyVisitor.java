import java.util.*;
import java.util.function.*;

import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

class TypeVisitor extends GJDepthFirst<Type, TypeEnv> {
    @Override
    public Type visit(ArrayType n, TypeEnv argu) {
        return Prim.ARR;
    }

    @Override
    public Type visit(BooleanType n, TypeEnv argu) {
        return Prim.BOOL;
    }

    @Override
    public Type visit(IntegerType n, TypeEnv argu) {
        return Prim.INT;
    }

    @Override
    public Type visit(Identifier n, TypeEnv argu) {
        final var className = n.f0.tokenImage;
        return argu.getClass(className);
    }
}

class SymPairVisitor extends GJDepthFirst<SymPair, TypeEnv> {
    @Override
    public SymPair visit(VarDeclaration n, TypeEnv argu) {
        final var typeNode = n.f0.f0.choice;
        final var sym = n.f1.f0.tokenImage;
        final var type = typeNode.accept(new TypeVisitor(), argu);
        return new SymPair(sym, type);
    }

    @Override
    public SymPair visit(FormalParameter n, TypeEnv argu) {
        final var typeNode = n.f0.f0.choice;
        final var sym = n.f1.f0.tokenImage;
        final var type = typeNode.accept(new TypeVisitor(), argu);
        return new SymPair(sym, type);
    }
}

class ParamListVisitor extends GJDepthFirst<List<SymPair>, TypeEnv> {
    @Override
    public List<SymPair> visit(FormalParameterList n, TypeEnv argu) {
        final var firstParamNode = n.f0;
        final var restParamNodes = n.f1.nodes;
        final var firstSymPair = firstParamNode.accept(new SymPairVisitor(), argu);
        return restParamNodes.stream().reduce(new List<>(firstSymPair, null),
                (params, node) -> new List<>(node.accept(new SymPairVisitor(), argu), params), (u, v) -> v);
    }
}

class MethodVisitor extends GJDepthFirst<Method, TypeEnv> {
    @Override
    public Method visit(MethodDeclaration n, TypeEnv argu) {
        final var retTypeNode = n.f1.f0.choice;
        final var name = n.f2.f0.tokenImage;
        final var paramList = n.f4;

        if (!paramList.present())
            return new Method(name, null, retTypeNode.accept(new TypeVisitor(), argu));
        else
            return new Method(name, paramList.node.accept(new ParamListVisitor(), argu),
                    retTypeNode.accept(new TypeVisitor(), argu));
    }
}

class TypeDeclVisitor extends GJDepthFirst<Class, Lazy<TypeEnv>> {
    @Override
    public Class visit(TypeDeclaration n, Lazy<TypeEnv> argu) {
        return n.f0.accept(this, argu);
    }

    @Override
    public Class visit(ClassDeclaration n, Lazy<TypeEnv> argu) {
        final var className = n.f1.f0.tokenImage;
        final var fieldNodes = n.f3.nodes;
        final var methodNodes = n.f4.nodes;
        return visitFieldsMethods(fieldNodes, methodNodes, className, new Lazy<>(() -> null), argu);
    }

    @Override
    public Class visit(ClassExtendsDeclaration n, Lazy<TypeEnv> argu) {
        final var className = n.f1.f0.tokenImage;
        final var superName = n.f3.f0.tokenImage;
        final var fieldNodes = n.f5.nodes;
        final var methodNodes = n.f6.nodes;
        return visitFieldsMethods(fieldNodes, methodNodes, className, new Lazy<>(() -> argu.get().getClass(superName)),
                argu);
    }

    Class visitFieldsMethods(Vector<Node> fieldNodes, Vector<Node> methodNodes, String className, Lazy<Class> superName,
            Lazy<TypeEnv> argu) {
        final var fields = new Lazy<List<SymPair>>(() -> fieldNodes.stream().reduce(null,
                (next, node) -> new List<>(node.accept(new SymPairVisitor(), argu.get()), next),
                (u, v) -> v));

        final var methods = new Lazy<List<Method>>(() -> methodNodes.stream().reduce(null,
                (next, node) -> new List<>(node.accept(new MethodVisitor(), argu.get()), next),
                (u, v) -> v));

        return new Class(className, fields, methods, superName);
    }
}

class MyVisitor extends GJDepthFirst<Optional<Type>, TypeEnv> {
    @Override
    public Optional<Type> visit(Goal n, TypeEnv argu) {
        final var typeDecls = n.f1.nodes;

        final var typeEnv = new Lazy<>((Lazy<TypeEnv> z) -> typeDecls.stream().reduce(new TypeEnv(null, null, null),
                (e, node) -> e.cons(node.accept(new TypeDeclVisitor(), z)), (u, v) -> v)).get();

        typeEnv.classList.foreach(Class::eval);



        return super.visit(n, argu);
    }
}

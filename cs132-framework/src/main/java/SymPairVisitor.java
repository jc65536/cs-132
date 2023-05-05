import java.util.Optional;

import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

class TypeVisitor extends GJDepthFirst<Optional<Type>, TypeEnv> {
    @Override
    public Optional<Type> visit(cs132.minijava.syntaxtree.Type n, TypeEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Optional<Type> visit(ArrayType n, TypeEnv argu) {
        return Optional.of(Prim.ARR);
    }

    @Override
    public Optional<Type> visit(BooleanType n, TypeEnv argu) {
        return Optional.of(Prim.BOOL);
    }

    @Override
    public Optional<Type> visit(IntegerType n, TypeEnv argu) {
        return Optional.of(Prim.INT);
    }

    @Override
    public Optional<Type> visit(Identifier n, TypeEnv argu) {
        final var className = n.f0.tokenImage;
        return Optional.of(argu.classLookup(className));
    }
}

public class SymPairVisitor extends GJDepthFirst<Optional<SymPair>, TypeEnv> {
    @Override
    public Optional<SymPair> visit(VarDeclaration n, TypeEnv argu) {
        final var name = n.f1.f0.tokenImage;
        return n.f0.accept(new TypeVisitor(), argu)
            .map(type -> new SymPair(name, type));
    }

    @Override
    public Optional<SymPair> visit(FormalParameter n, TypeEnv argu) {
        final var name = n.f1.f0.tokenImage;
        return n.f0.accept(new TypeVisitor(), argu)
            .map(type -> new SymPair(name, type));
    }

    @Override
    public Optional<SymPair> visit(FormalParameterRest n, TypeEnv argu) {
        return n.f1.accept(this, argu);
    }
}

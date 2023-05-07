import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

class TypeVisitor extends GJDepthFirst<Type, TypeEnv> {
    @Override
    public Type visit(cs132.minijava.syntaxtree.Type n, TypeEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

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
        return argu.classLookup(className);
    }
}

public class SymPairVisitor extends GJDepthFirst<SymPair, TypeEnv> {
    @Override
    public SymPair visit(VarDeclaration n, TypeEnv argu) {
        final var name = n.f1.f0.tokenImage;
        final var type = n.f0.accept(new TypeVisitor(), argu);
        return new SymPair(name, type);
    }

    @Override
    public SymPair visit(FormalParameter n, TypeEnv argu) {
        final var name = n.f1.f0.tokenImage;
        final var type = n.f0.accept(new TypeVisitor(), argu);
        return new SymPair(name, type);
    }

    @Override
    public SymPair visit(FormalParameterRest n, TypeEnv argu) {
        return n.f1.accept(this, argu);
    }
}

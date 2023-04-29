import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

public class TypeVisitor extends GJDepthFirst<Type, TypeEnv> {
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

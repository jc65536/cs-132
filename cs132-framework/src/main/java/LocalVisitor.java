import java.util.function.Function;

import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

class TypeVisitor extends GJDepthFirst<Type, TypeEnv> {
    @Override
    public Type visit(cs132.minijava.syntaxtree.Type n, TypeEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Type visit(ArrayType n, TypeEnv argu) {
        return Type.PRIM;
    }

    @Override
    public Type visit(BooleanType n, TypeEnv argu) {
        return Type.PRIM;
    }

    @Override
    public Type visit(IntegerType n, TypeEnv argu) {
        return Type.PRIM;
    }

    @Override
    public Type visit(Identifier n, TypeEnv argu) {
        final var className = n.f0.tokenImage;
        return argu.classLookup(className);
    }
}

class FieldVisitor extends GJDepthFirst<Function<Integer, Field>, TypeEnv> {
    @Override
    public Function<Integer, Field> visit(VarDeclaration n, TypeEnv argu) {
        final var name = n.f1.f0.tokenImage;
        final var type = n.f0.accept(new TypeVisitor(), argu);
        return fieldOffset -> new Field(name, type, fieldOffset);
    }
}

public class LocalVisitor extends GJDepthFirst<Local, TypeEnv> {
    @Override
    public Local visit(VarDeclaration n, TypeEnv argu) {
        final var name = n.f1.f0.tokenImage;
        final var type = n.f0.accept(new TypeVisitor(), argu);
        return new Local(name, type, new cs132.IR.token.Identifier("v_" + name));
    }

    @Override
    public Local visit(FormalParameter n, TypeEnv argu) {
        final var name = n.f1.f0.tokenImage;
        final var type = n.f0.accept(new TypeVisitor(), argu);
        return new Local(name, type, new cs132.IR.token.Identifier("p_" + name));
    }

    @Override
    public Local visit(FormalParameterRest n, TypeEnv argu) {
        return n.f1.accept(this, argu);
    }
}

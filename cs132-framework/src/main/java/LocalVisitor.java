import java.util.*;
import java.util.function.*;

import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;
import cs132.IR.token.Identifier;

// In HW2 I had a Type super class which included primitives, but here an
// Optional<Class> is enough to express all the type information I need.

class TypeVisitor extends GJDepthFirst<Optional<Class>, TypeEnv> {
    @Override
    public Optional<Class> visit(cs132.minijava.syntaxtree.Type n, TypeEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Optional<Class> visit(ArrayType n, TypeEnv argu) {
        return Optional.empty();
    }

    @Override
    public Optional<Class> visit(BooleanType n, TypeEnv argu) {
        return Optional.empty();
    }

    @Override
    public Optional<Class> visit(IntegerType n, TypeEnv argu) {
        return Optional.empty();
    }

    @Override
    public Optional<Class> visit(cs132.minijava.syntaxtree.Identifier n, TypeEnv argu) {
        return Optional.of(argu.classLookup(n.f0.tokenImage));
    }
}

// Returning a Function<Integer, Field> instead of a Field directly for a
// similar reason as ClassVisitor.

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
        return new Local(name, type, new Identifier("v_" + name));
    }

    @Override
    public Local visit(FormalParameter n, TypeEnv argu) {
        final var name = n.f1.f0.tokenImage;
        final var type = n.f0.accept(new TypeVisitor(), argu);
        return new Local(name, type, new Identifier("v_" + name));
    }

    @Override
    public Local visit(FormalParameterRest n, TypeEnv argu) {
        return n.f1.accept(this, argu);
    }
}

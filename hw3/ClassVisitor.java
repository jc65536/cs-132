import java.util.*;
import java.util.function.*;

import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

class MethodVisitor extends GJDepthFirst<Method, T2<Class, TypeEnv>> {
    @Override
    public Method visit(MethodDeclaration n, T2<Class, TypeEnv> argu) {
        return argu.then(c -> env -> {
            final var params = n.f4.accept(new ListVisitor<>(new LocalVisitor()), env);
            final var name = n.f2.f0.tokenImage;
            final var locals = n.f7.accept(new ListVisitor<>(new LocalVisitor()), env);
            final var retType = n.f1.accept(new TypeVisitor(), env);
            return new Method(name, params, locals, retType, n, c);
        });
    }
}

// ListVisitor builds the list like a map operation - each list node is
// instantiated without the context of the previous nodes - which is why it can
// be lazy. I want to build the Class list as if by a fold operation - when
// instantiating the next Class object, I want to feed it information obtained
// from the previously built Class objects. This is I first create a list of
// functions, then fold that list into another list of Classes in J2S.java. The
// Lazy<Integer> required to complete each Class is the global vtable offset
// for that class's vtables, which is calculated from the previous Class's
// vtableEnd.

// I often use the ListVisitor then fold method to compute iteratively. Other
// use cases include visiting fields (each Field needs an offset, which is
// gotten from the last visited Field) and translating (each translation
// procedure needs to cons onto the previous translation history).

public class ClassVisitor extends GJDepthFirst<Function<Lazy<Integer>, Class>, Lazy<TypeEnv>> {
    @Override
    public Function<Lazy<Integer>, Class> visit(TypeDeclaration n, Lazy<TypeEnv> argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Function<Lazy<Integer>, Class> visit(ClassDeclaration n, Lazy<TypeEnv> argu) {
        final var className = n.f1.f0.tokenImage;
        return vtableOffset -> new Class(className, Optional.empty(), n.f3, n.f4, vtableOffset, argu);
    }

    @Override
    public Function<Lazy<Integer>, Class> visit(ClassExtendsDeclaration n, Lazy<TypeEnv> argu) {
        final var className = n.f1.f0.tokenImage;
        final var superName = n.f3.f0.tokenImage;
        return vtableOffset -> new Class(className,
                Optional.of(argu.then(env -> env.classLookup(superName))),
                n.f5, n.f6, vtableOffset, argu);
    }
}

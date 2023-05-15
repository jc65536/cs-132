import java.util.*;
import java.util.function.Function;

import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

class MethodVisitor extends GJDepthFirst<Method, T2<Class, TypeEnv>> {
    @Override
    public Method visit(MethodDeclaration n, T2<Class, TypeEnv> argu) {
        final var c = argu.a;
        final var env = argu.b;
        final var params = n.f4.accept(new ListVisitor<>(new LocalVisitor()), env);
        final var name = n.f2.f0.tokenImage;
        final var locals = n.f7.accept(new ListVisitor<>(new LocalVisitor()), env);
        final var retType = n.f1.accept(new TypeVisitor(), env);
        return new Method(name, params, locals, retType, n,
                () -> c.superClass()
                        .flatMap(sc -> sc.methodLookup(name))
                        .map(u -> OverrideStatus.OVERRIDES)
                        .orElseGet(() -> env.classes.filter(clas -> clas != c && clas.subtypes(c))
                                .flatMap(clas -> clas.methods.get())
                                .find(m -> m.name.equals(name))
                                .map(u -> OverrideStatus.OVERRIDDEN)
                                .orElse(OverrideStatus.UNIQUE)),
                c);
    }
}

public class ClassVisitor extends GJDepthFirst<Function<List<Class>, Class>, Lazy<? extends TypeEnv>> {
    @Override
    public Function<List<Class>, Class> visit(TypeDeclaration n, Lazy<? extends TypeEnv> argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Function<List<Class>, Class> visit(ClassDeclaration n, Lazy<? extends TypeEnv> argu) {
        final var className = n.f1.f0.tokenImage;
        return classAcc -> new Class(className,
                Optional.empty(),
                (c) -> mkFields(c, n.f3, argu.get()),
                (c) -> mkMethods(c, n.f4, argu.get()),
                argu);
    }

    @Override
    public Function<List<Class>, Class> visit(ClassExtendsDeclaration n, Lazy<? extends TypeEnv> argu) {
        final var className = n.f1.f0.tokenImage;
        final var superName = n.f3.f0.tokenImage;
        return classAcc -> new Class(className,
                Optional.of(() -> argu.get().classLookup(superName)),
                (c) -> mkFields(c, n.f5, argu.get()),
                (c) -> mkMethods(c, n.f6, argu.get()),
                argu);
    }

    static <T extends TypeEnv> List<Field> mkFields(Class c, NodeListOptional fieldNodes, T argu) {
        return fieldNodes.accept(new ListVisitor<>(new FieldVisitor()), argu)
                .fold(List.<Field>nul(),
                        (fieldAcc, mkField) -> fieldAcc
                                .cons(mkField.apply(c.ownFieldsOffset.get() + fieldAcc.count() * 4)));
    }

    static <T extends TypeEnv> List<Method> mkMethods(Class c, NodeListOptional methodNodes, T argu) {
        return methodNodes.accept(new ListVisitor<>(new MethodVisitor()), new T2<>(c, argu));
    }
}

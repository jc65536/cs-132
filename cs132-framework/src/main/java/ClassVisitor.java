import java.util.*;
import java.util.function.*;

import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

class MethodVisitor extends GJDepthFirst<Method, T2<Class, TypeEnv>> {
    @Override
    public Method visit(MethodDeclaration n, T2<Class, TypeEnv> argu) {
        final var c = argu.a;
        final var typeEnv = argu.b;
        final var params = n.f4.accept(new ListVisitor<>(new LocalVisitor()), typeEnv);
        final var name = n.f2.f0.tokenImage;
        final var locals = n.f7.accept(new ListVisitor<>(new LocalVisitor()), typeEnv);
        final var retType = n.f1.accept(new TypeVisitor(), typeEnv);
        return new Method(name, params, locals, retType, n,
                (m) -> c.superClass()
                        .flatMap(sc -> sc.methodLookup(name))
                        .<OverrideStatus>map(sm -> m.new Overrides(sm.status.get().origClass()))
                        .orElseGet(() -> typeEnv.classes.filter(cls -> cls != c && cls.subtypes(c))
                                .flatMap(cls -> cls.methods.get())
                                .find(sm -> sm.name.equals(name))
                                .<OverrideStatus>map(u -> m.new Overridden())
                                .orElse(m.new Unique())),
                c);
    }
}

public class ClassVisitor extends GJDepthFirst<Function<List<Class>, Class>, Lazy<TypeEnv>> {
    @Override
    public Function<List<Class>, Class> visit(TypeDeclaration n, Lazy<TypeEnv> argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Function<List<Class>, Class> visit(ClassDeclaration n, Lazy<TypeEnv> argu) {
        final var className = n.f1.f0.tokenImage;
        return classAcc -> new Class(className,
                Optional.empty(),
                (c) -> mkFields(c, n.f3, argu.get()),
                (c) -> mkMethods(c, n.f4, argu.get()),
                (c) -> mkVtables(c, argu.get()),
                argu);
    }

    @Override
    public Function<List<Class>, Class> visit(ClassExtendsDeclaration n, Lazy<TypeEnv> argu) {
        final var className = n.f1.f0.tokenImage;
        final var superName = n.f3.f0.tokenImage;
        return classAcc -> new Class(className,
                Optional.of(() -> argu.get().classLookup(superName)),
                (c) -> mkFields(c, n.f5, argu.get()),
                (c) -> mkMethods(c, n.f6, argu.get()),
                (c) -> mkVtables(c, argu.get()),
                argu);
    }

    static List<Field> mkFields(Class c, NodeListOptional fieldNodes, TypeEnv argu) {
        return fieldNodes.accept(new ListVisitor<>(new FieldVisitor()), argu)
                .fold(List.<Field>nul(),
                        (fieldAcc, mkField) -> fieldAcc
                                .cons(mkField.apply(c.ownFieldsOffset.get() + fieldAcc.count() * 4)));
    }

    static List<Method> mkMethods(Class c, NodeListOptional methodNodes, TypeEnv argu) {
        return methodNodes.accept(new ListVisitor<>(new MethodVisitor()), new T2<>(c, argu));
    }

    static List<Vtable> mkVtables(Class c, TypeEnv env) {
        final var overridenVtables = c.superClass()
                .map(sc -> sc.vtables.get().map(vt -> {
                    final var overridingMethods = c.overridingMethods.get()
                            .filter(m -> m.status.get().origClass() == vt.target);

                    return overridingMethods.head()
                            .map(u -> new Vtable(vt.target,
                                    vt.overrides.map(m -> overridingMethods.get().find(m::nameEquals).orElse(m)),
                                    env))
                            .orElse(vt);
                }))
                .orElse(List.nul());

        return c.overriddenMethods.get().head()
                .map(u -> overridenVtables.cons(new Vtable(c, c.overriddenMethods.get(), env)))
                .orElse(overridenVtables);
    }
}

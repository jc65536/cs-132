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
        return new Method(name, params, locals, retType, n, c,
                (m) -> c.superClass()
                        .flatMap(sc -> sc.methodLookup(name))
                        .<OverrideStatus>map(sm -> m.new Overrides(sm.status.get().origClass()))
                        .orElseGet(() -> typeEnv.classes
                                .filter(cls -> cls != c && cls.subtypes(c))
                                .flatMap(cls -> cls.methods)
                                .find(m::nameEquals)
                                .<OverrideStatus>map(u -> m.new Overridden())
                                .orElseGet(() -> m.new Unique())));
    }
}

public class ClassVisitor extends GJDepthFirst<Function<Lazy<Integer>, Class>, Lazy<TypeEnv>> {
    @Override
    public Function<Lazy<Integer>, Class> visit(TypeDeclaration n, Lazy<TypeEnv> argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Function<Lazy<Integer>, Class> visit(ClassDeclaration n, Lazy<TypeEnv> argu) {
        final var className = n.f1.f0.tokenImage;
        return vtableOffset -> new Class(className,
                Optional.empty(),
                (c) -> mkFields(c, n.f3, argu.get()),
                (c) -> mkMethods(c, n.f4, argu.get()),
                (c) -> mkVtables(c, argu.get(), vtableOffset.get()));
    }

    @Override
    public Function<Lazy<Integer>, Class> visit(ClassExtendsDeclaration n, Lazy<TypeEnv> argu) {
        final var className = n.f1.f0.tokenImage;
        final var superName = n.f3.f0.tokenImage;
        return vtableOffset -> new Class(className,
                Optional.of(() -> argu.get().classLookup(superName)),
                (c) -> mkFields(c, n.f5, argu.get()),
                (c) -> mkMethods(c, n.f6, argu.get()),
                (c) -> mkVtables(c, argu.get(), vtableOffset.get()));
    }

    static List<Field> mkFields(Class c, NodeListOptional fieldNodes, TypeEnv argu) {
        int fieldOffset = c.ownObjOffset.get() + c.overriddenMethods.get().head().map(u -> 4).orElse(0);
        return fieldNodes.accept(new ListVisitor<>(new FieldVisitor()), argu).fold(List.<Field>nul(),
                (fieldAcc, mkField) -> fieldAcc.cons(mkField.apply(fieldOffset + fieldAcc.count() * 4)));
    }

    static List<Method> mkMethods(Class c, NodeListOptional methodNodes, TypeEnv argu) {
        return methodNodes.accept(new ListVisitor<>(new MethodVisitor()), new T2<>(c, argu));
    }

    static T3<Lazy<List<OverridingMethod>>, Lazy<List<OverriddenMethod>>, Lazy<List<UniqueMethod>>> mkMethods(Class c,
            NodeListOptional methodNodes, TypeEnv argu, int u) {
        return methodNodes.accept(new ListVisitor<>(new MethodVisitor()), new T2<>(c, argu))
                .fold(new T3<>(new Lazy<>(() -> List.<OverridingMethod>nul()),
                        new Lazy<>(() -> List.<OverriddenMethod>nul()),
                        new Lazy<>(() -> List.<UniqueMethod>nul())),
                        (acc, m) -> {
                            final var overriding = acc.a;
                            final var overridden = acc.b;
                            final var unique = acc.c;
                            return null;
                        });
    }

    static List<Vtable> mkVtables(Class c, TypeEnv env, int vtableOffset) {
        return c.superClass()
                .map(sc -> sc.vtables.get())
                .orElse(List.nul())
                .fold(new T2<>(vtableOffset, List.<Vtable>nul()), (acc, vt) -> {
                    final var offset = acc.a;
                    final var list = acc.b;

                    final var overridingMethods = c.overridingMethods.get()
                            .filter(m -> m.status.get().origClass() == vt.target);

                    return overridingMethods.head()
                            .map(u -> new Vtable(vt.target, vt.overrides
                                    .map(m -> overridingMethods.find(m::nameEquals).orElse(m)),
                                    offset))
                            .map(newVt -> new T2<>(offset + newVt.size, list.cons(newVt)))
                            .orElse(new T2<>(offset, list.cons(vt)));
                })
                .consume((nextVtableOffset, overriddenVtables) -> c.overriddenMethods.get().head()
                        .map(u -> overriddenVtables.cons(new Vtable(c, c.overriddenMethods.get(), nextVtableOffset)))
                        .orElse(overriddenVtables));
    }
}

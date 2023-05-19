import java.util.*;
import java.util.function.*;

import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

class MethodVisitor extends GJDepthFirst<Method, T2<Class, TypeEnv>> {
    @Override
    public Method visit(MethodDeclaration n, T2<Class, TypeEnv> argu) {
        return argu.consume(c -> env -> {
            final var params = n.f4.accept(new ListVisitor<>(new LocalVisitor()), env);
            final var name = n.f2.f0.tokenImage;
            final var locals = n.f7.accept(new ListVisitor<>(new LocalVisitor()), env);
            final var retType = n.f1.accept(new TypeVisitor(), env);
            return new Method(name, params, locals, retType, n, c);
        });
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
                (ms) -> mkStruct(ms, argu.get()),
                (c) -> mkVtables(c, argu.get(), vtableOffset));
    }

    @Override
    public Function<Lazy<Integer>, Class> visit(ClassExtendsDeclaration n, Lazy<TypeEnv> argu) {
        final var className = n.f1.f0.tokenImage;
        final var superName = n.f3.f0.tokenImage;
        return vtableOffset -> new Class(className,
                Optional.of(() -> argu.get().classLookup(superName)),
                (c) -> mkFields(c, n.f5, argu.get()),
                (c) -> mkMethods(c, n.f6, argu.get()),
                (ms) -> mkStruct(ms, argu.get()),
                (c) -> mkVtables(c, argu.get(), vtableOffset));
    }

    static T2<List<Field>, Integer> mkFields(Class c, NodeListOptional fieldNodes, TypeEnv argu) {
        final var fieldOffset = c.ownObjOffset.get() + c.methods.overridden.head().map(u -> 4).orElse(0);
        return fieldNodes.accept(new ListVisitor<>(new FieldVisitor()), argu)
                .fold(new T2<>(List.<Field>nul(), fieldOffset), (acc, mkField) -> acc
                        .consume(fieldAcc -> mkField.andThen(f -> new T2<>(fieldAcc.cons(f), f.offset + 4))));
    }

    static List<Method> mkMethods(Class c, NodeListOptional methodNodes, TypeEnv argu) {
        return methodNodes.accept(new ListVisitor<>(new MethodVisitor()), new T2<>(c, argu));
    }

    static T2<List<Vtable>, Lazy<Integer>> mkVtables(Class c, TypeEnv env, Lazy<Integer> vtableOffset) {
        return c.superClass().map(sc -> sc.vtables).orElse(List.nul())
                .fold(new T2<>(List.<Vtable>nul(), vtableOffset),
                        (acc, vt) -> acc.consume(list -> offset -> {
                            final var overridingMethods = c.methods.overriding
                                    .filter(m -> m.origin() == vt.target);

                            return overridingMethods.head()
                                    .map(u -> vt.overrides.map(m -> overridingMethods
                                            .find(m::nameEquals).<Virtual>map(x -> x).orElse(m)))
                                    .map(overrides -> new Vtable(vt.target, overrides, offset))
                                    .map(newVt -> new T2<>(list.cons(newVt),
                                            new Lazy<>(() -> offset.get() + vt.size)))
                                    .orElse(new T2<>(list.cons(vt), offset));
                        }))
                .consume(overriddenVtables -> nextOffset -> c.methods.overridden.head()
                        .map(u -> new Vtable(c, c.methods.overridden, nextOffset))
                        .map(newVt -> new T2<>(overriddenVtables.cons(newVt),
                                new Lazy<>(() -> nextOffset.get() + newVt.size)))
                        .orElse(new T2<>(overriddenVtables, nextOffset)));
    }

    static MethodStruct mkStruct(List<Method> methods, TypeEnv env) {
        return methods.fold(new MethodStruct(methods, List.nul(), List.nul(), List.nul()),
                (struct, m) -> m.c.superClass()
                        .flatMap(sc -> sc.classifiedLookup(m.name))
                        .map(sm -> struct.cons(new Overriding(m, sm.origin())))
                        .or(() -> env.classes
                                .filter(cls -> cls != m.c && cls.subtypes(m.c))
                                .flatMap(cls -> cls.methods.all)
                                .find(m::nameEquals)
                                .map(u -> struct.cons(new Overridden(m))))
                        .orElseGet(() -> struct.cons(new Unique(m))));
    }
}

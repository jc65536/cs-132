import java.util.*;
import java.util.function.*;

import cs132.minijava.syntaxtree.*;

interface Type {
    default boolean subtypes(Type other) {
        return this == other;
    };
}

abstract class Named {
    protected final String name;

    Named(String name) {
        this.name = name;
    }

    static <T extends Named> Optional<List<T>> distinct(List<T> acc, Optional<T> nOpt) {
        return nOpt.filter(n -> !acc.exists(m -> m.name.equals(n.name))).map(acc::cons);
    }
}

enum Prim implements Type {
    INT, BOOL, ARR
}

class Class extends Named implements Type {
    private final Optional<Lazy<Class>> superClass;
    final Lazy<List<SymPair>> fields;
    final Lazy<List<Method>> methods;

    Class(String name, Optional<Supplier<Class>> superClass,
            Supplier<List<SymPair>> mkFields,
            Function<Class, List<Method>> mkMethods) {
        super(name);
        this.superClass = superClass.map(Lazy::new);
        this.fields = new Lazy<>(mkFields);
        this.methods = new Lazy<>(() -> mkMethods.apply(this));
    }

    Optional<Class> superClass() {
        return superClass.map(Lazy::get);
    }

    @Override
    public boolean subtypes(Type other) {
        return this == other || superClass().map(sc -> sc.subtypes(other)).orElse(false);
    }

    Optional<SymPair> fieldLookup(String name) {
        return fields.get()
                .find(s -> s.name.equals(name))
                .or(() -> superClass().flatMap(sc -> sc.fieldLookup(name)))
                .or(() -> Typecheck.error("Unknown field " + name));
    }

    Optional<Method> methodLookup(String name) {
        return methods.get()
                .find(m -> m.name.equals(name))
                .or(() -> superClass().flatMap(sc -> sc.methodLookup(name)))
                .or(() -> Typecheck.error("Unknown method " + name));
    }

    boolean acyclic(List<Class> h) {
        return !h.exists(this::equals) && superClass().map(sc -> sc.acyclic(h.cons(this))).orElse(true);
    }

    boolean noOverloading(Method method) {
        return superClass()
                .map(sc -> sc.methods.get().forAll(m -> !m.name.equals(method.name) || m.typeEquals(method))
                        && sc.noOverloading(method))
                .orElse(true);
    }
}

class SymPair extends Named {
    final Type type;

    SymPair(String name, Type type) {
        super(name);
        this.type = type;
    }
}

class Method extends Named {
    final List<SymPair> params;
    final Type retType;
    final MethodDeclaration body;

    Method(String name, List<SymPair> params, Type retType, MethodDeclaration body) {
        super(name);
        this.params = params;
        this.retType = retType;
        this.body = body;
    }

    boolean argsCompat(List<? extends Type> argTypes) {
        return argTypes.equals(params, (u, v) -> u.subtypes(v.type));
    }

    boolean typeEquals(Method other) {
        return retType == other.retType && params.equals(other.params, (u, v) -> u.type == v.type);
    }
}

public class TypeEnv {
    final List<SymPair> locals;
    final List<Class> classes;
    final Optional<Class> currClass;

    TypeEnv(List<SymPair> locals, List<Class> classes, Optional<Class> currClass) {
        this.locals = locals;
        this.classes = classes;
        this.currClass = currClass;
    }

    TypeEnv addLocals(List<SymPair> locals) {
        return new TypeEnv(this.locals.join(locals), classes, currClass);
    }

    TypeEnv enterClass(Class c) {
        return new TypeEnv(locals, classes, Optional.of(c));
    }

    Class classLookup(String name) {
        return classes.find(c -> c.name.equals(name))
                .or(() -> Typecheck.error("Unknown class " + name))
                .get();
    }

    Optional<SymPair> symLookup(String name) {
        return locals.find(s -> s.name.equals(name))
                .or(() -> currClass.flatMap(c -> c.fieldLookup(name)))
                .or(() -> Typecheck.error("Unknown symbol " + name));
    }
}

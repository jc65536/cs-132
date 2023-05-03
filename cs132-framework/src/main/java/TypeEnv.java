import java.util.*;
import java.util.function.*;

import cs132.minijava.syntaxtree.*;

interface Type {
    boolean subtypes(Type other);
}

interface Named {
    String name();

    static boolean distinct(List<? extends Named> list, Named n) {
        return !list.exists(m -> m.name().equals(n.name()));
    }
}

enum Prim implements Type {
    INT, BOOL, ARR;

    @Override
    public boolean subtypes(Type other) {
        return this == other;
    }
}

class Class implements Named, Type {
    private final String name;
    private final Optional<Lazy<Class>> superClass;
    private final Lazy<ClassBody> body;

    Class(String name, Optional<Supplier<Class>> superClass, Function<Class, ClassBody> body) {
        this.name = name;
        this.superClass = superClass.map(Lazy::new);
        this.body = new Lazy<>(() -> body.apply(this));
    }

    ClassBody body() {
        return body.get();
    }

    Optional<Class> superClass() {
        return superClass.map(Lazy::get);
    }

    @Override
    public boolean subtypes(Type other) {
        return this == other || superClass().map(sc -> sc.subtypes(other)).orElse(false);
    }

    Optional<SymPair> fieldLookup(String name) {
        return body().fields
                .find(s -> s.name().equals(name))
                .or(() -> superClass().flatMap(sc -> sc.fieldLookup(name)))
                .or(() -> Typecheck.error("Unknown field " + name));
    }

    Optional<Method> methodLookup(String name) {
        return body().methods
                .find(m -> m.name().equals(name))
                .or(() -> superClass().flatMap(sc -> sc.methodLookup(name)))
                .or(() -> Typecheck.error("Unknown method " + name));
    }

    boolean acyclic(List<Class> h) {
        return !h.exists(this::equals) && superClass().map(sc -> sc.acyclic(h.cons(this))).orElse(true);
    }

    boolean noOverloading(Method method) {
        return superClass()
                .map(sc -> sc.body().methods.forAll(m -> !m.name().equals(method.name()) || m.typeEquals(method))
                        && sc.noOverloading(method))
                .orElse(true);
    }

    @Override
    public String toString() {
        return String.format("%s%s", name(), superClass().map(sc -> ": " + sc.toString()).orElse(""));
    }

    @Override
    public String name() {
        return name;
    }
}

class ClassBody {
    final List<SymPair> fields;
    final List<Method> methods;

    ClassBody(List<SymPair> fields, List<Method> methods) {
        this.fields = fields;
        this.methods = methods;
    }

    @Override
    public String toString() {
        return String.format("%s%s",
                fields.fold("", (str, f) -> String.format("%s%s\n", str, f)),
                methods.fold("", (str, m) -> String.format("%s%s\n", str, m)));
    }
}

class SymPair implements Named {
    private final String name;
    final Type type;

    SymPair(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", name, type);
    }

    @Override
    public String name() {
        return name;
    }
}

class Method implements Named {
    private final String name;
    final List<SymPair> params;
    final Type retType;
    final MethodDeclaration body;

    Method(String name, List<SymPair> params, Type retType, MethodDeclaration body) {
        this.name = name;
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

    @Override
    public String toString() {
        return String.format("%s: %s%s",
                name,
                params.fold("", (str, p) -> String.format("%s%s -> ", str, p)),
                retType);
    }

    @Override
    public String name() {
        return name;
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

    TypeEnv enterClassMethod(Class c, Method m) {
        return new TypeEnv(m.params, classes, Optional.of(c));
    }

    Class classLookup(String name) {
        return classes
                .find(c -> c.name().equals(name))
                .or(() -> Typecheck.error("Unknown class " + name))
                .get();
    }

    Optional<SymPair> symLookup(String name) {
        return locals
                .find(s -> s.name().equals(name))
                .or(() -> currClass.flatMap(c -> c.fieldLookup(name)))
                .or(() -> Typecheck.error("Unknown symbol " + name));
    }

    @Override
    public String toString() {
        return classes.fold("", (str, c) -> String.format("%s---\n%s\n%s", str, c, c.body()));
    }
}

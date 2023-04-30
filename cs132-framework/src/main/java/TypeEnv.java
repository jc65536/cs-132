
import java.util.*;
import java.util.function.*;

import cs132.minijava.syntaxtree.*;

interface Type {
    boolean subtypes(Type other);
}

interface Named {
    String name();

    static boolean distinct(List<? extends Named> list, Named named) {
        return !list.exists(n -> n.name().equals(named.name()));
    }
}

enum Prim implements Type {
    INT, BOOL, ARR;

    @Override
    public boolean subtypes(Type other) {
        return this == other;
    }
}

class Class extends Lazy<ClassBody> implements Type, Named {
    private final String name;

    Class(String name, Supplier<ClassBody> body) {
        super(body);
        this.name = name;
    }

    public boolean subtypes(Type other) {
        return other instanceof Class
                && (this == other || this.get().superClass
                        .map(sc -> sc.subtypes(other))
                        .orElse(false));
    }

    SymPair fieldLookup(String sym) {
        return get().fields
                .find(s -> s.name().equals(sym))
                .or(() -> get().superClass.map(sc -> sc.fieldLookup(sym)))
                .orElseGet(() -> Util.error("Unknown field " + sym));
    }

    Method methodLookup(String name, List<Type> paramTypes) {
        return get().methods
                .find(m -> m.name().equals(name) && m.argsCompat(paramTypes))
                .or(() -> get().superClass.map(sc -> sc.methodLookup(name, paramTypes)))
                .orElseGet(() -> Util.error("Unknown method " + name));
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String name() {
        return name;
    }
}

class ClassBody {
    final List<SymPair> fields;
    final List<Method> methods;
    final Optional<Class> superClass;

    ClassBody(List<SymPair> fields, List<Method> methods, Optional<Class> superClass) {
        this.fields = fields;
        this.methods = methods;
        this.superClass = superClass;
    }

    @Override
    public String toString() {
        return String.format("%s%s%s",
                superClass.map(sc -> sc.toString() + "\n").orElse(""),
                fields.fold("", (str, f) -> String.format("%s%s\n", str, f)),
                methods.fold("", (str, m) -> String.format("%s%s\n", str, m)));
    }
}

class SymPair implements Named {
    private final String sym;
    final Type type;

    SymPair(String sym, Type type) {
        this.sym = sym;
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", sym, type);
    }

    @Override
    public String name() {
        return sym;
    }
}

class Method implements Named {
    private final String name;
    final List<SymPair> params;
    final Type retType;
    final Node body;

    Method(String name, List<SymPair> params, Type retType, MethodDeclaration body) {
        this.name = name;
        this.params = params;
        this.retType = retType;
        this.body = body;
    }

    boolean argsCompat(List<Type> argTypes) {
        return argTypes.equals(params, (u, v) -> u.subtypes(v.type));
    }

    boolean typeEquals(Method other) {
        return retType == other.retType && params.equals(other.params, (u, v) -> u.type == v.type);
    }

    @Override
    public String toString() {
        return String.format("%s: %s -> %s",
                name,
                params.fold("", (str, p) -> String.format("%s, %s", str, p)),
                retType);
    }

    @Override
    public String name() {
        return name;
    }
}

public class TypeEnv {
    final List<SymPair> symList;
    final List<Class> classList;
    final Optional<Class> currClass;
    final Optional<Method> currMethod;

    TypeEnv(List<SymPair> symList, List<Class> classList, Optional<Class> currClass, Optional<Method> currMethod) {
        this.symList = symList;
        this.classList = classList;
        this.currClass = currClass;
        this.currMethod = currMethod;
    }

    TypeEnv addLocals(List<SymPair> locals) {
        return new TypeEnv(symList.join(locals), classList, currClass, currMethod);
    }

    TypeEnv enterClassMethod(Class c, Method m) {
        return new TypeEnv(m.params, classList, Optional.of(c), Optional.of(m));
    }

    Class classLookup(String name) {
        return classList
                .find(c -> c.name().equals(name))
                .orElseGet(() -> Util.error("Unknown class " + name));
    }

    SymPair symLookup(String sym) {
        return symList
                .find(s -> s.name().equals(sym))
                .or(() -> currClass.map(c -> c.fieldLookup(sym)))
                .orElseGet(() -> Util.error("Unknown symbol " + sym));
    }

    @Override
    public String toString() {
        return classList.fold("", (str, c) -> String.format("%s---\n%s\n%s", str, c, c.get()));
    }
}

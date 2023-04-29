
import java.util.*;
import java.util.function.*;

import cs132.IR.syntaxtree.Node;
import cs132.minijava.syntaxtree.MethodDeclaration;

interface Type {
    boolean subtypes(Type other);
}

enum Prim implements Type {
    INT, BOOL, ARR;

    @Override
    public boolean subtypes(Type other) {
        return this == other;
    }
}

class Class extends Lazy<ClassBody> implements Type {
    final String name;

    Class(String name, Supplier<ClassBody> body) {
        super(body);
        this.name = name;
    }

    public boolean subtypes(Type other) {
        return other instanceof Class
                && (this == other || this.get().superClass.map(sc -> sc.subtypes(other)).orElse(false));
    }

    SymPair fieldLookup(String sym) {
        return get().fields.find(s -> s.sym.equals(sym)).or(() -> get().superClass.map(sc -> sc.fieldLookup(sym)))
                .orElseGet(() -> Util.error("Unknown field " + sym));
    }

    Method methodLookup(String name, List<Type> paramTypes) {
        return get().methods.find(m -> m.name.equals(name) && m.argsCompatible(paramTypes))
                .orElseGet(() -> Util.error("Unknown method " + name));
    }
}

class ClassBody {
    final List<SymPair> fields;
    final List<Method> methods;
    final Optional<Class> superClass;

    ClassBody(List<SymPair> fields,
            List<Method> methods,
            Optional<Class> superClass) {
        this.fields = fields;
        this.methods = methods;
        this.superClass = superClass;
    }
}

class SymPair {
    final String sym;
    final Type type;

    SymPair(String sym, Type type) {
        this.sym = sym;
        this.type = type;
    }
}

class Method {
    final String name;
    final List<SymPair> params;
    final Type retType;
    final MethodDeclaration body;

    Method(String name, List<SymPair> params, Type retType, MethodDeclaration body) {
        this.name = name;
        this.params = params;
        this.retType = retType;
        this.body = body;
    }

    boolean argsCompatible(List<Type> argTypes) {
        return argTypes.equals(params, (u, v) -> u.subtypes(v.type));
    }

    boolean typeEquals(Method other) {
        return retType == other.retType && params.equals(other.params, (u, v) -> u.type == v.type);
    }
}

public class TypeEnv {
    final List<SymPair> symList;
    final List<Class> classList;
    final Optional<Class> currClass;

    TypeEnv(List<SymPair> symList, List<Class> classList, Optional<Class> currClass) {
        this.symList = symList;
        this.classList = classList;
        this.currClass = currClass;
    }

    TypeEnv enterClass(Class c) {
        return new TypeEnv(List.nul(), classList, Optional.of(c));
    }

    TypeEnv enterMethod(Method m) {
        return new TypeEnv(m.params, classList, currClass);
    }

    Class classLookup(String name) {
        return classList.find(c -> c.name.equals(name)).orElseGet(() -> Util.error("Unknown class " + name));
    }

    SymPair symLookup(String sym) {
        return symList.find(s -> s.sym.equals(sym)).or(() -> currClass.map(c -> c.fieldLookup(sym)))
                .orElseGet(() -> Util.error("Unknown symbol " + sym));
    }
}

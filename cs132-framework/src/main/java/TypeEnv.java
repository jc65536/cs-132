import java.util.function.*;

interface Type {
}

enum Prim implements Type {
    INT, BOOL, ARR
}

class Class implements Type {
    final String name;
    final Lazy<List<SymPair>> fields;
    final Lazy<List<Method>> methods;
    final Lazy<Class> superClass;

    Class(String name,
            Lazy<List<SymPair>> fields,
            Lazy<List<Method>> methods,
            Lazy<Class> superClass) {
        this.fields = fields;
        this.methods = methods;
        this.name = name;
        this.superClass = superClass;
    }

    void eval() {
        fields.get();
        methods.get();
        superClass.get();
    }

    boolean subtypes(Class other) {
        return this == other || this.superClass.get().subtypes(other);
    }
}

class SymPair {
    public final String sym;
    public final Type type;

    public SymPair(String sym, Type type) {
        this.sym = sym;
        this.type = type;
    }
}

class Method {
    public final String name;
    public final List<SymPair> params;
    public final Type retType;

    public Method(String name, List<SymPair> params, Type retType) {
        this.name = name;
        this.params = params;
        this.retType = retType;
    }
}

class TypeEnv {
    public final List<SymPair> symList;
    public final List<Class> classList;
    public final Type currClass;

    public TypeEnv(List<SymPair> symList, List<Class> classList, Type currClass) {
        this.symList = symList;
        this.classList = classList;
        this.currClass = currClass;
    }

    public TypeEnv cons(SymPair pair) {
        return new TypeEnv(new List<>(pair, symList), classList, currClass);
    }

    public TypeEnv cons(Class clas) {
        return new TypeEnv(symList, new List<>(clas, classList), currClass);
    }

    public TypeEnv setCurrClass(Type currClass) {
        return new TypeEnv(symList, classList, currClass);
    }

    Class getClass(String name) {
        return classList.find(c -> c.name.equals(name)).get();
    }
}

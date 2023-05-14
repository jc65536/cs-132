import java.util.*;
import java.util.function.*;

import cs132.IR.sparrow.Call;
import cs132.IR.sparrow.FunctionDecl;
import cs132.IR.sparrow.Instruction;
import cs132.IR.sparrow.Load;
import cs132.IR.sparrow.Move_Id_FuncName;
import cs132.IR.sparrow.Move_Id_Id;
import cs132.IR.sparrow.Store;
import cs132.IR.token.FunctionName;
import cs132.IR.token.Identifier;
import cs132.IR.token.Label;
import cs132.minijava.syntaxtree.*;

interface Type {
    boolean subtypes(Type other);
}

abstract class Named {
    protected final String name;

    Named(String name) {
        this.name = name;
    }

    static <T extends Named> Optional<List<T>> distinct(List<T> acc, Optional<T> nOpt) {
        return nOpt.filter(n -> acc.forAll(m -> !m.name.equals(n.name))).map(acc::cons);
    }

    static <T extends Named> Optional<List<T>> distinct(List<T> acc, T n) {
        return distinct(acc, Optional.of(n));
    }

    boolean nameEquals(Named n) {
        return name.equals(n.name);
    }
}

enum Prim implements Type {
    INT, BOOL, ARR;

    @Override
    public boolean subtypes(Type other) {
        return this == other;
    }
}

class Class extends Named implements Type {
    private final Optional<Lazy<Class>> superClass;
    final Lazy<List<Field>> fields;
    final Lazy<List<Method>> methods;
    final Lazy<? extends TypeEnv> env;

    // Offset into static memory to entire vtable
    final Lazy<Integer> vtableOffset;

    // Offset into static memory to own vtable
    final Lazy<Integer> ownVtableOffset;

    // Size of own vtable
    final Lazy<Integer> ownVtableSize;

    // Size of entire vtable
    final Lazy<Integer> vtableSize;

    // Size of entire object
    final Lazy<Integer> objSize;

    // Offset into object to own data
    final Lazy<Integer> ownObjOffset;

    // Ofset into object to own fields
    final Lazy<Integer> ownFieldsOffset;

    Class(String name,
            Optional<? extends Supplier<Class>> superClass,
            Function<Class, List<Field>> mkFields,
            Function<Class, List<Method>> mkMethods,
            Lazy<? extends TypeEnv> env,
            Supplier<Integer> vtableOffset) {
        super(name);
        this.superClass = superClass.map(Lazy::new);
        fields = new Lazy<>(() -> mkFields.apply(this));
        methods = new Lazy<>(() -> mkMethods.apply(this));
        this.env = env;

        this.vtableOffset = new Lazy<>(vtableOffset);

        this.ownVtableOffset = new Lazy<>(
                () -> superClass().map(sc -> sc.vtableSize.get()).orElse(0) + vtableOffset.get());

        ownVtableSize = new Lazy<>(
                () -> methods.get().filter(m -> m.status.get() == OverrideStatus.OVERRIDDEN).count() * 4);

        this.vtableSize = new Lazy<>(() -> superClass().map(sc -> sc.vtableSize.get()).orElse(0) + ownVtableSize.get());

        ownObjOffset = new Lazy<>(() -> superClass().map(sc -> sc.objSize.get()).orElse(0));

        ownFieldsOffset = new Lazy<>(() -> ownObjOffset.get() + (ownVtableSize.get() > 0 ? 4 : 0));

        objSize = new Lazy<>(() -> ownFieldsOffset.get() + fields.get().count() * 4);
    }

    Optional<Class> superClass() {
        return superClass.map(Lazy::get);
    }

    @Override
    public boolean subtypes(Type other) {
        return this == other || superClass().map(sc -> sc.subtypes(other)).orElse(false);
    }

    Optional<Field> fieldLookup(String name) {
        return fields.get()
                .find(s -> s.name.equals(name))
                .or(() -> superClass().flatMap(sc -> sc.fieldLookup(name)));
    }

    Optional<Method> methodLookup(String name) {
        return methods.get()
                .find(m -> m.name.equals(name))
                .or(() -> superClass().flatMap(sc -> sc.methodLookup(name)));
    }

    T2<Integer, TransEnv> mkVtable(Identifier stat, Identifier tSym, int offset, TransEnv env, List<Method> overrides) {
        final var overridenMethods = methods.get().filter(m -> m.status.get() == OverrideStatus.OVERRIDDEN);
        final var overridingMethods = methods.get().filter(m -> m.status.get() == OverrideStatus.OVERRIDES);

        final var t1 = overrides.partition(m -> overridenMethods.exists(n -> m.name.equals(n.name)));
        final var selfOverrides = t1.a;
        final var superOverrides = t1.b;

        final var env2 = env.join(List.of(J2S.comment("Vtable for " + name)));

        final var t2 = superClass()
                .map(sc -> sc.mkVtable(stat, tSym, offset, env2, superOverrides.join(overridingMethods)))
                .orElse(new T2<>(offset, env2));

        return overridenMethods.fold(t2,
                (acc, m) -> {
                    final var tOffset = acc.a;
                    final var tEnv = acc.b;
                    final var methodEntry = selfOverrides.find(m::nameEquals).orElse(m);
                    return new T2<>(tOffset + 4, tEnv.join(List.<Instruction>nul()
                            .cons(new Store(stat, tOffset, tSym))
                            .cons(new Move_Id_FuncName(tSym, methodEntry.funcName()))));
                });
    }
}

abstract class Variable extends Named {
    final Type type;

    Variable(String name, Type type) {
        super(name);
        this.type = type;
    }

    abstract T2<Identifier, TransEnv> toTemp(TransEnv env);

    abstract TransEnv assign(Identifier src, TransEnv env);
}

class Field extends Variable {
    final int offset;

    Field(String name, Type type, int offset) {
        super(name, type);
        this.offset = offset;
    }

    @Override
    T2<Identifier, TransEnv> toTemp(TransEnv argu) {
        final var t = argu.genSym();
        final var sym = t.a;
        final var env = t.b;
        return new T2<>(sym, env.join(List.of(new Load(sym, TransEnv.thisSym(), offset))));
    }

    @Override
    TransEnv assign(Identifier src, TransEnv env) {
        return env.join(List.of(new Store(TransEnv.thisSym(), offset, src)));
    }
}

class Local extends Variable {
    final Identifier sym;

    Local(String name, Type type, Identifier sym) {
        super(name, type);
        this.sym = sym;
    }

    @Override
    T2<Identifier, TransEnv> toTemp(TransEnv argu) {
        return new T2<>(sym, argu);
    }

    @Override
    TransEnv assign(Identifier dest, TransEnv env) {
        return env.join(List.of(new Move_Id_Id(dest, this.sym)));
    }
}

enum OverrideStatus {
    OVERRIDES, OVERRIDDEN, UNIQUE
}

class Method extends Named {
    final List<Local> params;
    final Type retType;
    final MethodDeclaration body;
    final Lazy<OverrideStatus> status;
    final Lazy<Integer> vtableOffset;
    final Class c;

    Method(String name, List<Local> params, Type retType, MethodDeclaration body, Supplier<OverrideStatus> status,
            Class c, Supplier<Integer> vtableOffset) {
        super(name);
        this.params = params;
        this.retType = retType;
        this.body = body;
        this.status = new Lazy<>(status);
        this.c = c;
        this.vtableOffset = new Lazy<>(vtableOffset);
    }

    Method setStatus(Supplier<OverrideStatus> status) {
        return new Method(name, params, retType, body, status, c, vtableOffset);
    }

    Method setClass(Class c) {
        return new Method(name, params, retType, body, status, c, vtableOffset);
    }

    Method setOffset(Supplier<Integer> offset) {
        return new Method(name, params, retType, body, status, c, offset);
    }

    boolean argsCompat(List<? extends Type> argTypes) {
        return argTypes.equals(params, (u, v) -> u.subtypes(v.type));
    }

    boolean typeEquals(Method other) {
        return retType == other.retType && params.equals(other.params, (u, v) -> u.type == v.type);
    }

    FunctionDecl translate() {
        return new FunctionDecl(funcName(), List.<Identifier>nul().toJavaList(),
                new cs132.IR.sparrow.Block(List.<Instruction>nul().toJavaList(), new Identifier("ret")));
    }

    T2<Identifier, TransEnv> call(Identifier stat, Identifier thisSym, List<Identifier> restArgs, TransEnv pEnv) {
        final var t1 = pEnv.genSym();
        final var eSym = t1.a;
        final var env = t1.b;

        final var args = restArgs.cons(thisSym).cons(stat);

        switch (status.get()) {
            case OVERRIDDEN:
            case OVERRIDES:
                return new T2<>(eSym, env.join(List.<Instruction>nul()
                        .cons(new Call(eSym, eSym, args.toJavaList()))
                        .cons(new Load(eSym, eSym, vtableOffset.get()))
                        .cons(new Load(eSym, thisSym, c.ownObjOffset.get()))));
            case UNIQUE:
            default:
                return new T2<>(eSym, env.join(List.<Instruction>nul()
                        .cons(new Call(eSym, eSym, args.toJavaList()))
                        .cons(new Move_Id_FuncName(eSym, funcName()))));
        }
    }

    FunctionName funcName() {
        return new FunctionName(c.name + "_" + name);
    }
}

public class TypeEnv {
    final List<Local> locals;
    final List<Class> classes;
    final Optional<Class> currClass;

    TypeEnv(List<Local> locals, List<Class> classes, Optional<Class> currClass) {
        this.locals = locals;
        this.classes = classes;
        this.currClass = currClass;
    }

    TypeEnv enterClass(Class c) {
        return new TypeEnv(locals, classes, Optional.of(c));
    }

    Class classLookup(String name) {
        return classes.find(c -> c.name.equals(name))
                .or(() -> J2S.error("Unknown class " + name))
                .get();
    }

    Optional<Variable> symLookup(String name) {
        return locals.find(s -> s.name.equals(name))
                .<Variable>map(x -> x)
                .or(() -> currClass.flatMap(c -> c.fieldLookup(name)))
                .or(() -> J2S.error("Unknown symbol " + name));
    }
}

class TransEnv extends TypeEnv {
    final List<Instruction> code;
    final int k;

    TransEnv(TypeEnv typeEnv, List<Instruction> code, int k) {
        super(typeEnv.locals, typeEnv.classes, typeEnv.currClass);
        this.code = code;
        this.k = k;
    }

    TransEnv inc() {
        return new TransEnv(this, code, k + 1);
    }

    T2<Identifier, TransEnv> genSym() {
        return new T2<>(new Identifier("t" + k), inc());
    }

    T2<Label, TransEnv> genLabel() {
        return new T2<>(new Label("L" + k), inc());
    }

    TransEnv join(List<Instruction> c) {
        return new TransEnv(this, code.join(c), k);
    }

    TransEnv addLocal(Local pair) {
        return new TransEnv(new TypeEnv(locals.cons(pair), classes, currClass), code, k);
    }

    static Identifier thisSym() {
        return new Identifier("this");
    }
}

import java.util.*;
import java.util.function.*;

import cs132.IR.sparrow.Add;
import cs132.IR.sparrow.Alloc;
import cs132.IR.sparrow.Call;
import cs132.IR.sparrow.FunctionDecl;
import cs132.IR.sparrow.Instruction;
import cs132.IR.sparrow.Load;
import cs132.IR.sparrow.Move_Id_FuncName;
import cs132.IR.sparrow.Move_Id_Id;
import cs132.IR.sparrow.Move_Id_Integer;
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

class Vtable {
    final Class target;
    final List<Method> overrides;
    final int size;
    final Lazy<Integer> offset;

    Vtable(Class target, List<Method> overrides, Lazy<? extends TypeEnv> env) {
        this.target = target;
        this.overrides = overrides;
        this.size = overrides.count() * 4;
        this.offset = new Lazy<>(() -> env.get().vtables.get().find(t -> this == t.a).get().b);
    }

    @Override
    public String toString() {
        return String.format("%s: %s (%s)", target.name, overrides.fold("", (s, m) -> s + m.toString() + ", "),
                this.hashCode());
    }

    TransEnv write(Identifier stat, Identifier tSym, TransEnv env) {
        env = env.join(List.of(J2S.comment("Vtable_for_" + target.name)));

        return overrides.fold(new T2<>(offset.get(), env), (acc, m) -> {
            final var offset = acc.a;
            final var tEnv = acc.b;
            return new T2<>(offset + 4, tEnv.join(List.<Instruction>nul()
                    .cons(new Store(stat, offset, tSym))
                    .cons(new Move_Id_FuncName(tSym, m.funcName()))));
        }).b;
    }
}

class Class extends Named implements Type {
    private final Optional<Lazy<Class>> superClass;
    final Lazy<List<Field>> fields;
    final Lazy<List<Method>> methods;
    final Lazy<List<Method>> overriddenMethods;
    final Lazy<List<Method>> overridingMethods;
    final Lazy<? extends TypeEnv> env;

    // Size of own vtable
    final Lazy<Integer> ownVtableSize;

    // Size of entire vtable
    final Lazy<Integer> vtableSize;

    // Size of entire object
    final Lazy<Integer> objSize;

    final Lazy<Integer> ownObjSize;

    // Offset into object to own data
    final Lazy<Integer> ownObjOffset;

    // Ofset into object to own fields
    final Lazy<Integer> ownFieldsOffset;

    final Lazy<List<Vtable>> vtables;

    Class(String name,
            Optional<? extends Supplier<Class>> superClass,
            Function<Class, List<Field>> mkFields,
            Function<Class, List<Method>> mkMethods,
            Lazy<? extends TypeEnv> env) {
        super(name);
        this.superClass = superClass.map(Lazy::new);
        fields = new Lazy<>(() -> mkFields.apply(this));
        methods = new Lazy<>(() -> mkMethods.apply(this));
        overriddenMethods = new Lazy<>(() -> methods.get().filter(m -> m.status.get() == OverrideStatus.OVERRIDDEN));
        overridingMethods = new Lazy<>(() -> methods.get().filter(m -> m.status.get() == OverrideStatus.OVERRIDES));
        this.env = env;

        ownVtableSize = new Lazy<>(
                () -> overriddenMethods.get().count() * 4);

        this.vtableSize = new Lazy<>(() -> superClass().map(sc -> sc.vtableSize.get()).orElse(0) + ownVtableSize.get());

        ownObjOffset = new Lazy<>(() -> superClass().map(sc -> sc.objSize.get()).orElse(0));

        ownFieldsOffset = new Lazy<>(() -> ownObjOffset.get() + (ownVtableSize.get() > 0 ? 4 : 0));

        ownObjSize = new Lazy<>(() -> overriddenMethods.get().head().map(u -> 4).orElse(0)
                + fields.get().count() * 4);

        objSize = new Lazy<>(() -> ownFieldsOffset.get() + fields.get().count() * 4);

        vtables = new Lazy<>(() -> {
            final var overridenVtables = superClass()
                    .map(sc -> sc.vtables.get()
                            .map(vt -> vt.overrides
                                    .find(m -> overridingMethods.get().exists(m::nameEquals))
                                    .map(u -> new Vtable(vt.target, vt.overrides
                                            .map(m -> overridingMethods.get().find(m::nameEquals).orElse(m)),
                                            env))
                                    .orElse(vt)))
                    .orElse(List.nul());

            return overriddenMethods.get().head()
                    .map(u -> overridenVtables.cons(new Vtable(this, overriddenMethods.get(), env)))
                    .orElse(overridenVtables);
        });
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

    T2<Identifier, TransEnv> alloc(TransEnv argu) {
        final var t1 = argu.genSym();
        final var obj = t1.a;
        final var t2 = t1.b.genSym();
        final var tSym = t2.a;
        final var env = t2.b;
        return new T2<>(obj, initialize(obj, tSym,
                env.join(List.<Instruction>of(new Alloc(obj, tSym))
                        .cons(new Move_Id_Integer(tSym, objSize.get())))));
    }

    TransEnv initialize(Identifier obj, Identifier tSym, TransEnv argu) {
        final var env = superClass().map(sc -> sc.initialize(obj, tSym, argu)).orElse(argu);

        return vtables.get()
                .find(vt -> vt.target.equals(this))
                .map(vt -> env.join(List.<Instruction>nul()
                        .cons(new Add(obj, obj, tSym))
                        .cons(new Move_Id_Integer(tSym, ownObjSize.get()))
                        .cons(new Store(obj, 0, tSym))
                        .cons(new Load(tSym, TransEnv.statSym, vt.offset.get()))))
                .orElse(env);
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
        return env.join(List.of(new Move_Id_Id(sym, dest)));
    }
}

enum OverrideStatus {
    OVERRIDES, OVERRIDDEN, UNIQUE
}

class Method extends Named {
    final List<Local> params;
    final List<Local> locals;
    final Type retType;
    final MethodDeclaration body;
    final Lazy<OverrideStatus> status;
    final Class c;

    Method(String name,
            List<Local> params,
            List<Local> locals,
            Type retType,
            MethodDeclaration body,
            Supplier<OverrideStatus> status,
            Class c) {
        super(name);
        this.params = params;
        this.locals = locals;
        this.retType = retType;
        this.body = body;
        this.status = new Lazy<>(status);
        this.c = c;
    }

    Method setStatus(Supplier<OverrideStatus> status) {
        return new Method(name, params, locals, retType, body, status, c);
    }

    Method setClass(Class c) {
        return new Method(name, params, locals, retType, body, status, c);
    }

    boolean argsCompat(List<? extends Type> argTypes) {
        return argTypes.equals(params, (u, v) -> u.subtypes(v.type));
    }

    boolean typeEquals(Method other) {
        return retType == other.retType && params.equals(other.params, (u, v) -> u.type == v.type);
    }

    FunctionDecl translate(TypeEnv typeEnv) {
        final var typeEnv2 = typeEnv.addLocals(params).addLocals(locals);
        final var transEnv = new TransEnv(List.nul(), 0);

        final var p = body.f8.accept(new FoldVisitor<>(new StmtVisitor(), te -> new T2<>(typeEnv2, te)),
                new T2<>(typeEnv2, transEnv));

        final var retExpr = body.f10.accept(new ExprVisitor(), new T2<>(typeEnv2, p.b));

        return new FunctionDecl(funcName(),
                params.map(s -> s.sym).cons(TransEnv.thisSym()).cons(TransEnv.statSym).toJavaList(),
                new cs132.IR.sparrow.Block(retExpr.c.code.toJavaList(), retExpr.a));
    }

    T2<Identifier, TransEnv> call(Identifier thisSym, List<Identifier> restArgs, TransEnv pEnv) {
        final var t1 = pEnv.genSym();
        final var eSym = t1.a;
        final var env = t1.b;

        final var args = restArgs.cons(thisSym).cons(TransEnv.statSym);

        switch (status.get()) {
            case OVERRIDDEN:
            case OVERRIDES:
                return new T2<>(eSym, env.join(List.<Instruction>nul()
                        .cons(new Call(eSym, eSym, args.toJavaList()))
                        .cons(new Load(eSym, eSym, 0))
                        .cons(new Load(eSym, thisSym, c.ownObjOffset.get()))));
            case UNIQUE:
            default:
                return new T2<>(eSym, env.join(List.<Instruction>nul()
                        .cons(new Call(eSym, eSym, args.toJavaList()))
                        .cons(new Move_Id_FuncName(eSym, funcName()))));
        }
    }

    FunctionName funcName() {
        return new FunctionName(toString());
    }

    @Override
    public String toString() {
        return c.name + "_" + name;
    }
}

public class TypeEnv {
    final List<Local> locals;
    final List<Class> classes;
    final Optional<Class> currClass;
    final Lazy<List<T2<Vtable, Integer>>> vtables;

    TypeEnv(List<Local> locals, List<Class> classes, Optional<Class> currClass) {
        this.locals = locals;
        this.classes = classes;
        this.currClass = currClass;
        this.vtables = new Lazy<>(() -> classes
                .flatMap(c -> c.vtables.get())
                .unique(Object::equals)
                .fold(new T2<>(List.<T2<Vtable, Integer>>nul(), 0), (acc, vt) -> {
                    final var list = acc.a;
                    final var offset = acc.b;
                    return new T2<>(list.cons(new T2<>(vt, offset)), offset + vt.size);
                }).a);
    }

    TypeEnv enterClass(Class c) {
        return new TypeEnv(locals, classes, Optional.of(c));
    }

    Class classLookup(String name) {
        return classes.find(c -> c.name.equals(name))
                .or(() -> J2S.error("Unknown class " + name))
                .get();
    }

    TypeEnv addLocals(List<Local> locals) {
        return new TypeEnv(this.locals.join(locals), classes, currClass);
    }

    Optional<Variable> symLookup(String name) {
        return locals.find(s -> s.name.equals(name))
                .<Variable>map(x -> x)
                .or(() -> currClass.flatMap(c -> c.fieldLookup(name)))
                .or(() -> J2S.error("Unknown symbol " + name));
    }
}

class TransEnv {
    final List<Instruction> code;
    final int k;

    TransEnv(List<Instruction> code, int k) {
        this.code = code;
        this.k = k;
    }

    TransEnv inc() {
        return new TransEnv(code, k + 1);
    }

    T2<Identifier, TransEnv> genSym() {
        return new T2<>(new Identifier("v" + k), inc());
    }

    T2<Label, TransEnv> genLabel() {
        return new T2<>(new Label("L" + k), inc());
    }

    TransEnv join(List<Instruction> c) {
        return new TransEnv(code.join(c), k);
    }

    static Identifier thisSym() {
        return new Identifier("this");
    }

    static final Identifier statSym = new Identifier("__stat__");
}

import java.util.*;
import java.util.function.*;

import cs132.IR.sparrow.*;
import cs132.IR.token.*;
import cs132.IR.token.Identifier;
import cs132.minijava.syntaxtree.*;

interface Type {
    boolean subtypes(Type other);
}

abstract class Named {
    protected final String name;

    Named(String name) {
        this.name = name;
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
    final Integer offset;

    Vtable(Class target, List<Method> overrides, int offset) {
        this.target = target;
        this.overrides = overrides;
        this.size = overrides.count() * 4;
        this.offset = offset;
    }

    TransEnv write(Identifier stat, Identifier tSym, TransEnv env) {
        env = env.join(List.of(J2S.comment(String.format("vtable_for_%s", target.name))));
        return overrides.fold(env, (acc, m) -> {
            return acc.join(List.<Instruction>nul()
                    .cons(new Store(stat, offset + ((Method.Vtabled) m.status.get()).offset.get(), tSym))
                    .cons(new Move_Id_FuncName(tSym, m.funcName())));
        });
    }
}

class Class extends Named implements Type {
    private final Optional<Lazy<Class>> superClass;
    final Lazy<List<Field>> fields;
    final Lazy<List<Method>> methods;
    final Lazy<List<Method>> overriddenMethods;
    final Lazy<List<Method>> overridingMethods;

    // Size of entire vtable
    final Lazy<Integer> vtableSize;

    // Size of entire object
    final Lazy<Integer> objSize;

    // Offset into object to own data
    final Lazy<Integer> ownObjOffset;

    final Lazy<List<Vtable>> vtables;

    Class(String name,
            Optional<? extends Supplier<Class>> superClass,
            Function<Class, List<Field>> mkFields,
            Function<Class, List<Method>> mkMethods,
            Function<Class, List<Vtable>> mkVtables) {
        super(name);
        this.superClass = superClass.map(Lazy::new);
        fields = new Lazy<>(() -> mkFields.apply(this));
        methods = new Lazy<>(() -> mkMethods.apply(this));

        overriddenMethods = new Lazy<>(() -> methods.get()
                .filter(m -> m.status.get() instanceof Method.Overridden));

        overridingMethods = new Lazy<>(() -> methods.get()
                .filter(m -> m.status.get() instanceof Method.Overrides));

        vtableSize = new Lazy<>(() -> superClass().map(sc -> sc.vtableSize.get()).orElse(0)
                + overriddenMethods.get().count() * 4);

        ownObjOffset = new Lazy<>(() -> superClass().map(sc -> sc.objSize.get()).orElse(0));

        objSize = new Lazy<>(() -> ownObjOffset.get()
                + overriddenMethods.get().head().map(u -> 4).orElse(0)
                + fields.get().count() * 4);

        vtables = new Lazy<>(() -> mkVtables.apply(this));
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
        return argu.genSym((obj, env1) -> env1.genSym((tmp, env2) -> {
            return new T2<>(obj, initialize(obj, tmp,
                    env2.join(List.<Instruction>nul()
                            .cons(new Alloc(obj, tmp))
                            .cons(new Move_Id_Integer(tmp, objSize.get()))),
                    vtables.get()));
        }));
    }

    TransEnv initialize(Identifier obj, Identifier tSym, TransEnv argu, List<Vtable> vtables) {
        final var env = superClass().map(sc -> sc.initialize(obj, tSym, argu, vtables)).orElse(argu);

        return vtables.find(vt -> vt.target.equals(this))
                .map(vt -> env.join(List.<Instruction>nul()
                        .cons(new Store(obj, ownObjOffset.get(), tSym))
                        .cons(new Add(tSym, TransEnv.statSym, tSym))
                        .cons(new Move_Id_Integer(tSym, vt.offset))))
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
    T2<Identifier, TransEnv> toTemp(TransEnv env) {
        return env.genSym((tmp, env1) -> new T2<>(tmp, env1.join(List.<Instruction>nul()
                .cons(new Load(tmp, TransEnv.thisSym, offset)))));
    }

    @Override
    TransEnv assign(Identifier src, TransEnv env) {
        return env.join(List.of(new Store(TransEnv.thisSym, offset, src)));
    }
}

class Local extends Variable {
    final Identifier sym;

    Local(String name, Type type, Identifier sym) {
        super(name, type);
        this.sym = sym;
    }

    @Override
    T2<Identifier, TransEnv> toTemp(TransEnv env) {
        return env.genSym((tmp, env1) -> new T2<>(tmp,
                env1.join(List.of(new Move_Id_Id(tmp, sym)))));
    }

    @Override
    TransEnv assign(Identifier src, TransEnv env) {
        return env.join(List.of(new Move_Id_Id(sym, src)));
    }
}

interface OverrideStatus {
    T2<Identifier, TransEnv> call(Identifier thisSym,
            List<Identifier> args,
            Identifier eSym,
            TransEnv env);

    Class origClass();
}

class Method extends Named {
    final List<Local> params;
    final List<Local> locals;
    final Type retType;
    final MethodDeclaration body;
    final Lazy<OverrideStatus> status;
    final Class c;

    class Unique implements OverrideStatus {
        @Override
        public T2<Identifier, TransEnv> call(Identifier thisSym,
                List<Identifier> args,
                Identifier eSym,
                TransEnv env) {
            return new T2<>(eSym, env.join(List.<Instruction>nul()
                    .cons(new Call(eSym, eSym, args.toJavaList()))
                    .cons(new Move_Id_FuncName(eSym, funcName()))));
        }

        @Override
        public Class origClass() {
            return c;
        }
    }

    abstract class Vtabled implements OverrideStatus {
        final Lazy<Integer> offset;

        public Vtabled() {
            this.offset = new Lazy<>(() -> c.vtables.get()
                    .find(vt -> origClass() == vt.target).get().overrides
                    .firstIndex(Method.this::equals).get() * 4);
        }

        @Override
        public T2<Identifier, TransEnv> call(Identifier thisSym,
                List<Identifier> args,
                Identifier eSym,
                TransEnv env) {
            return new T2<>(eSym, env.join(List.<Instruction>nul()
                    .cons(new Call(eSym, eSym, args.toJavaList()))
                    .cons(new Load(eSym, eSym, offset.get()))
                    .cons(new Load(eSym, thisSym, c.ownObjOffset.get()))));
        }
    }

    class Overridden extends Vtabled {
        @Override
        public Class origClass() {
            return c;
        }
    }

    class Overrides extends Vtabled {
        final Class target;

        Overrides(Class target) {
            this.target = target;
        }

        @Override
        public Class origClass() {
            return target;
        }
    }

    Method(String name,
            List<Local> params,
            List<Local> locals,
            Type retType,
            MethodDeclaration body,
            Class c,
            Function<Method, OverrideStatus> mkStatus) {
        super(name);
        this.params = params;
        this.locals = locals;
        this.retType = retType;
        this.body = body;
        this.status = new Lazy<>(() -> mkStatus.apply(this));
        this.c = c;
    }

    FunctionDecl translate(TypeEnv typeEnv) {
        final var localsEnv = typeEnv.addLocals(params).addLocals(locals);
        final var transEnv = new TransEnv(List.nul(), 0);

        final var p = body.f8.accept(new FoldVisitor<>(new StmtVisitor(),
                (acc, env) -> new T2<>(localsEnv, env)),
                new T2<>(localsEnv, transEnv));

        final var retExpr = body.f10.accept(new ExprVisitor(), new T2<>(localsEnv, p.b));

        return new FunctionDecl(funcName(),
                params.map(s -> s.sym).cons(TransEnv.thisSym).cons(TransEnv.statSym).toJavaList(),
                new cs132.IR.sparrow.Block(retExpr.c.code.toJavaList(), retExpr.a));
    }

    T2<Identifier, TransEnv> call(Identifier thisSym, List<Identifier> args, TransEnv env) {
        return env.genSym((res, env1) -> status.get().call(thisSym, args, res, env1));
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
    final Lazy<List<Vtable>> vtables;

    TypeEnv(List<Local> locals, List<Class> classes, Optional<Class> currClass) {
        this.locals = locals;
        this.classes = classes;
        this.currClass = currClass;
        this.vtables = new Lazy<>(() -> classes.flatMap(c -> c.vtables.get()).unique(Object::equals));
    }

    TypeEnv enterClass(Class c) {
        return new TypeEnv(locals, classes, Optional.of(c));
    }

    Class classLookup(String name) {
        return classes.find(c -> c.name.equals(name)).get();
    }

    TypeEnv addLocals(List<Local> locals) {
        return new TypeEnv(this.locals.join(locals), classes, currClass);
    }

    Optional<Variable> symLookup(String name) {
        return locals.find(s -> s.name.equals(name))
                .<Variable>map(x -> x)
                .or(() -> currClass.flatMap(c -> c.fieldLookup(name)));
    }
}

class TransEnv {
    final List<Instruction> code;
    private final int k;

    TransEnv(List<Instruction> code, int k) {
        this.code = code;
        this.k = k;
    }

    TransEnv inc() {
        return new TransEnv(code, k + 1);
    }

    <T> T genSym(BiFunction<Identifier, TransEnv, T> cont) {
        return cont.apply(new Identifier("v" + k), inc());
    }

    <T> T genLabel(BiFunction<Label, TransEnv, T> cont) {
        return cont.apply(new Label("L" + k), inc());
    }

    TransEnv join(List<Instruction> c) {
        return new TransEnv(code.join(c), k);
    }

    TransEnv nullCheck(Identifier obj) {
        return genLabel((err, env1) -> env1.genLabel((end, env2) -> {
            return env2.join(List.<Instruction>nul()
                    .cons(new LabelInstr(end))
                    .cons(new ErrorMessage("\"null pointer\""))
                    .cons(new LabelInstr(err))
                    .cons(new Goto(end))
                    .cons(new IfGoto(obj, err)));
        }));
    }

    TransEnv idxCheck(Identifier arr, Identifier idx) {
        return genSym((tmp, env1) -> env1.genLabel((err, env2) -> env2.genLabel((end, env3) -> {
            return env3.join(List.<Instruction>nul()
                    .cons(new LabelInstr(end))
                    .cons(new ErrorMessage("\"array index out of bounds\""))
                    .cons(new LabelInstr(err))
                    .cons(new Goto(end))
                    .cons(new IfGoto(tmp, err))
                    .cons(new LessThan(tmp, idx, tmp))
                    .cons(new Load(tmp, arr, 0)));
        })));
    }

    static final Identifier thisSym = new Identifier("this");

    static final Identifier statSym = new Identifier("__stat__");
}

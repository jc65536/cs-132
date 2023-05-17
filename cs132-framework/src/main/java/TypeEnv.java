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
    PRIMITIVE;

    @Override
    public boolean subtypes(Type other) {
        return this == other;
    }
}

class Vtable {
    final Class target;
    final List<Method> overrides;
    final int size;
    final int offset;

    Vtable(Class target, List<Method> overrides, int offset) {
        this.target = target;
        this.overrides = overrides;
        this.size = overrides.count() * 4;
        this.offset = offset;
    }

    TransEnv write(Identifier stat, Identifier tSym, TransEnv env) {
        env = env.cons(J2S.comment(String.format("vtable_for_%s", target.name)));
        return overrides.fold(env, (acc, m) -> {
            return acc.cons(new Move_Id_FuncName(tSym, m.funcName()))
                    .cons(new Store(stat, offset + ((Method.Vtabled) m.status.get()).offset.get(), tSym));
        });
    }
}

class Class extends Named implements Type {
    private final Optional<Lazy<Class>> superClass;
    final List<Field> fields;
    final List<Method> methods;
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
        fields = new List<>(() -> mkFields.apply(this).get());
        methods = new List<>(() -> mkMethods.apply(this).get());

        overriddenMethods = new Lazy<>(() -> methods.filter(m -> m.status.get() instanceof Method.Overridden));

        overridingMethods = new Lazy<>(() -> methods.filter(m -> m.status.get() instanceof Method.Overrides));

        vtableSize = new Lazy<>(() -> superClass().map(sc -> sc.vtableSize.get()).orElse(0)
                + overriddenMethods.get().count() * 4);

        ownObjOffset = new Lazy<>(() -> superClass().map(sc -> sc.objSize.get()).orElse(0));

        objSize = new Lazy<>(() -> ownObjOffset.get()
                + overriddenMethods.get().head().map(u -> 4).orElse(0)
                + fields.count() * 4);

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
        return fields.find(s -> s.name.equals(name))
                .or(() -> superClass().flatMap(sc -> sc.fieldLookup(name)));
    }

    Optional<Method> methodLookup(String name) {
        return methods.find(m -> m.name.equals(name))
                .or(() -> superClass().flatMap(sc -> sc.methodLookup(name)));
    }

    T2<Identifier, TransEnv> alloc(TransEnv argu) {
        return argu.genSym((obj, env1) -> env1.genSym((tmp, env2) -> {
            return new T2<>(obj, initialize(obj, tmp,
                    env2.cons(new Move_Id_Integer(tmp, objSize.get())).cons(new Alloc(obj, tmp)),
                    vtables.get()));
        }));
    }

    TransEnv initialize(Identifier obj, Identifier tSym, TransEnv argu, List<Vtable> vtables) {
        final var env = superClass().map(sc -> sc.initialize(obj, tSym, argu, vtables)).orElse(argu);

        return vtables.find(vt -> vt.target.equals(this))
                .map(vt -> env
                        .cons(new Move_Id_Integer(tSym, vt.offset))
                        .cons(new Add(tSym, TransEnv.statSym, tSym))
                        .cons(new Store(obj, ownObjOffset.get(), tSym)))
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
        return env.genSym((tmp, env1) -> new T2<>(tmp,
                env1.cons(new Load(tmp, TransEnv.thisSym, offset))));
    }

    @Override
    TransEnv assign(Identifier src, TransEnv env) {
        return env.cons(new Store(TransEnv.thisSym, offset, src));
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
                env1.cons(new Move_Id_Id(tmp, sym))));
    }

    @Override
    TransEnv assign(Identifier src, TransEnv env) {
        return env.cons(new Move_Id_Id(sym, src));
    }
}

interface OverrideStatus {
    T2<Identifier, TransEnv> call(Identifier thisSym,
            List<Identifier> args,
            Identifier eSym,
            TransEnv env);

    Class origClass();
}

abstract class VtabledMethod extends Method {
    final Lazy<Integer> offset;

    public VtabledMethod(String name, List<Local> params, List<Local> locals, Type retType, MethodDeclaration body,
            Class c, Function<Method, OverrideStatus> mkStatus) {
        super(name, params, locals, retType, body, c, mkStatus);
        this.offset = new Lazy<>(() -> c.vtables.get()
                .find(vt -> origClass() == vt.target).get().overrides
                .firstIndex(this::equals).get() * 4);
    }

    @Override
    public T2<Identifier, TransEnv> call(Identifier thisSym, List<Identifier> args, TransEnv env) {
        return env.genSym((eSym, env1) -> new T2<>(eSym, env1
                .cons(new Load(eSym, thisSym, c.ownObjOffset.get()))
                .cons(new Load(eSym, eSym, offset.get()))
                .cons(new Call(eSym, eSym, args.toJavaList()))));
    }
}

class OverridingMethod extends VtabledMethod {
    final Class target;

    OverridingMethod(String name, List<Local> params, List<Local> locals, Type retType, MethodDeclaration body, Class c,
            Function<Method, OverrideStatus> mkStatus, Class target) {
        super(name, params, locals, retType, body, c, mkStatus);
        this.target = target;
    }

    @Override
    Class origClass() {
        return target;
    }
}

class OverriddenMethod extends VtabledMethod {
    OverriddenMethod(String name, List<Local> params, List<Local> locals, Type retType, MethodDeclaration body, Class c,
            Function<Method, OverrideStatus> mkStatus) {
        super(name, params, locals, retType, body, c, mkStatus);
    }
}

class UniqueMethod extends Method {
    UniqueMethod(String name, List<Local> params, List<Local> locals, Type retType, MethodDeclaration body, Class c,
            Function<Method, OverrideStatus> mkStatus) {
        super(name, params, locals, retType, body, c, mkStatus);
    }

    @Override
    public T2<Identifier, TransEnv> call(Identifier thisSym, List<Identifier> args, TransEnv env) {
        return env.genSym((eSym, env1) -> new T2<>(eSym, env1
                .cons(new Move_Id_FuncName(eSym, funcName()))
                .cons(new Call(eSym, eSym, args.toJavaList()))));
    }
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
            return new T2<>(eSym, env
                    .cons(new Move_Id_FuncName(eSym, funcName()))
                    .cons(new Call(eSym, eSym, args.toJavaList())));
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
            return new T2<>(eSym, env
                    .cons(new Load(eSym, thisSym, c.ownObjOffset.get()))
                    .cons(new Load(eSym, eSym, offset.get()))
                    .cons(new Call(eSym, eSym, args.toJavaList())));
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
                new cs132.IR.sparrow.Block(retExpr.c.revCode.reverse().toJavaList(), retExpr.a));
    }

    T2<Identifier, TransEnv> call(Identifier thisSym, List<Identifier> args, TransEnv env) {
        return env.genSym((res, env1) -> status.get().call(thisSym, args, res, env1));
    }

    FunctionName funcName() {
        return new FunctionName(toString());
    }

    Class origClass() {
        return c;
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
    final List<Instruction> revCode;
    private final int k;

    TransEnv(List<Instruction> code, int k) {
        this.revCode = code;
        this.k = k;
    }

    TransEnv inc() {
        return new TransEnv(revCode, k + 1);
    }

    <T> T genSym(BiFunction<Identifier, TransEnv, T> cont) {
        return cont.apply(new Identifier("v" + k), inc());
    }

    <T> T genLabel(BiFunction<Label, TransEnv, T> cont) {
        return cont.apply(new Label("L" + k), inc());
    }

    TransEnv cons(Instruction i) {
        return new TransEnv(revCode.cons(i), k);
    }

    TransEnv nullCheck(Identifier obj) {
        return genLabel((err, env1) -> env1.genLabel((end, env2) -> {
            return env2.cons(new IfGoto(obj, err))
                    .cons(new Goto(end))
                    .cons(new LabelInstr(err))
                    .cons(new ErrorMessage("\"null pointer\""))
                    .cons(new LabelInstr(end));
        }));
    }

    TransEnv idxCheck(Identifier arr, Identifier idx) {
        return genSym((tmp, env1) -> env1.genLabel((err, env2) -> env2.genLabel((end, env3) -> {
            return env3.cons(new Load(tmp, arr, 0))
                    .cons(new LessThan(tmp, idx, tmp))
                    .cons(new IfGoto(tmp, err))
                    .cons(new Goto(end))
                    .cons(new LabelInstr(err))
                    .cons(new ErrorMessage("\"array index out of bounds\""))
                    .cons(new LabelInstr(end));
        })));
    }

    static final Identifier thisSym = new Identifier("this");

    static final Identifier statSym = new Identifier("__stat__");
}

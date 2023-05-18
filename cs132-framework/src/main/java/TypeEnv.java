import java.util.*;
import java.util.function.*;

import cs132.IR.sparrow.*;
import cs132.IR.token.*;
import cs132.IR.token.Identifier;
import cs132.minijava.syntaxtree.*;

class Expr extends Trans {
    final Identifier sym;
    final Optional<Class> type;

    private Expr(Trans tr, Identifier sym, Optional<Class> type) {
        super(tr.codeRev, tr.k);
        this.sym = sym;
        this.type = type;
    }

    static Function<Trans, Expr> make(Identifier sym, Optional<Class> type) {
        return tr -> new Expr(tr, sym, type);
    }

    Expr nullCheck() {
        return applyTo(Trans.genLabel(err -> Trans.genLabel(end -> tr -> tr
                .cons(new IfGoto(sym, err))
                .cons(new Goto(end))
                .cons(new LabelInstr(err))
                .cons(new ErrorMessage("\"null pointer\""))
                .cons(new LabelInstr(end)))))
                .applyTo(Expr.make(sym, type));
    }

    Expr idxCheck(Identifier arr) {
        return applyTo(Trans.genSym(len -> Trans.genLabel(err -> Trans.genLabel(end -> tr -> tr
                .cons(new Load(len, arr, 0))
                .cons(new LessThan(len, sym, len))
                .cons(new IfGoto(len, err))
                .cons(new Goto(end))
                .cons(new LabelInstr(err))
                .cons(new ErrorMessage("\"array index out of bounds\""))
                .cons(new LabelInstr(end))))))
                .applyTo(Expr.make(sym, type));
    }
}

abstract class Named {
    final String name;

    Named(String name) {
        this.name = name;
    }

    boolean nameEquals(Named n) {
        return name.equals(n.name);
    }
}

class Vtable {
    final Class target;
    final List<? extends Virtual> overrides;
    final int size;
    final int offset;

    Vtable(Class target, List<? extends Virtual> overrides, int offset) {
        this.target = target;
        this.overrides = overrides;
        this.size = overrides.count() * 4;
        this.offset = offset;
    }

    Trans write(Identifier tmp, Trans env) {
        env = env.cons(J2S.comment(String.format("Vtable_for_%s", target.name)));
        return overrides.fold(env, (acc, m) -> acc
                .cons(new Move_Id_FuncName(tmp, m.funcName()))
                .cons(new Store(Trans.stat, offset + m.offset.get(), tmp)));
    }
}

class Class extends Named {
    private final Optional<Lazy<Class>> superClass;
    final List<Field> fields;
    final MethodStruct methods;

    final List<Vtable> vtables;
    final Lazy<Integer> nextVtableOffset;

    final Lazy<Integer> objSize;
    final Lazy<Integer> ownObjOffset;

    Class(String name,
            Optional<? extends Supplier<Class>> superClass,
            Function<Class, List<Field>> mkFields,
            Function<Class, List<Method>> mkMethods,
            Function<List<Method>, MethodStruct> mkStruct,
            Function<Class, T2<Integer, List<Vtable>>> mkVtables) {
        super(name);
        this.superClass = superClass.map(Lazy::new);
        fields = new List<>(() -> mkFields.apply(this).get());

        final var all = new List<>(() -> mkMethods.apply(this).get());
        final var lazyStruct = new Lazy<>(() -> mkStruct.apply(all));
        methods = new MethodStruct(all,
                new List<>(() -> lazyStruct.get().overriding.get()),
                new List<>(() -> lazyStruct.get().overridden.get()),
                new List<>(() -> lazyStruct.get().unique.get()));

        final var lazyVtable = new Lazy<>(() -> mkVtables.apply(this));
        vtables = new List<>(() -> lazyVtable.get().b.get());
        nextVtableOffset = new Lazy<>(() -> lazyVtable.get().a);

        ownObjOffset = new Lazy<>(() -> superClass().map(sc -> sc.objSize.get()).orElse(0));

        objSize = new Lazy<>(() -> ownObjOffset.get()
                + methods.overridden.head().map(u -> 4).orElse(0)
                + fields.count() * 4);
    }

    Optional<Class> superClass() {
        return superClass.map(Lazy::get);
    }

    boolean subtypes(Class other) {
        return this == other || superClass().map(sc -> sc.subtypes(other)).orElse(false);
    }

    Optional<Field> fieldLookup(String name) {
        return fields.find(s -> s.name.equals(name))
                .or(() -> superClass().flatMap(sc -> sc.fieldLookup(name)));
    }

    Optional<Classified> classifiedLookup(String name) {
        return methods.classified.find(m -> m.name.equals(name))
                .or(() -> superClass().flatMap(sc -> sc.classifiedLookup(name)));
    }

    Function<Trans, Expr> alloc() {
        return Trans.genSym(obj -> Trans.genSym(tmp -> tr -> tr
                .cons(new Move_Id_Integer(tmp, objSize.get()))
                .cons(new Alloc(obj, tmp))
                .applyTo(init(obj, tmp, vtables))
                .applyTo(Expr.make(obj, Optional.of(this)))));
    }

    Function<Trans, Trans> init(Identifier obj, Identifier tmp, List<Vtable> vtables) {
        return superClass()
                .map(sc -> sc.init(obj, tmp, vtables))
                .orElse(x -> x)
                .andThen(tr -> vtables
                        .find(vt -> vt.target.equals(this))
                        .map(vt -> tr
                                .cons(new Move_Id_Integer(tmp, vt.offset))
                                .cons(new Add(tmp, Trans.stat, tmp))
                                .cons(new Store(obj, ownObjOffset.get(), tmp)))
                        .orElse(tr));
    }
}

abstract class Variable extends Named {
    final Optional<Class> type;

    Variable(String name, Optional<Class> type) {
        super(name);
        this.type = type;
    }

    abstract Function<Trans, Expr> toTemp();

    abstract Function<Trans, Trans> assign(Identifier src);
}

class Field extends Variable {
    final int offset;

    Field(String name, Optional<Class> type, int offset) {
        super(name, type);
        this.offset = offset;
    }

    @Override
    Function<Trans, Expr> toTemp() {
        return Trans.genSym(res -> tr -> tr.cons(new Load(res, Trans.self, offset))
                .applyTo(Expr.make(res, type)));
    }

    @Override
    Function<Trans, Trans> assign(Identifier src) {
        return tr -> tr.cons(new Store(Trans.self, offset, src));
    }
}

class Local extends Variable {
    final Identifier sym;

    Local(String name, Optional<Class> type, Identifier sym) {
        super(name, type);
        this.sym = sym;
    }

    @Override
    Function<Trans, Expr> toTemp() {
        return Trans.genSym(res -> tr -> tr.cons(new Move_Id_Id(res, sym))
                .applyTo(Expr.make(res, type)));
    }

    @Override
    Function<Trans, Trans> assign(Identifier src) {
        return tr -> tr.cons(new Move_Id_Id(sym, src));
    }
}

class MethodStruct {
    final List<Method> all;
    final List<Overriding> overriding;
    final List<Overridden> overridden;
    final List<Unique> unique;
    final List<Classified> classified;

    MethodStruct(List<Method> all,
            List<Overriding> overriding,
            List<Overridden> overridden,
            List<Unique> unique) {
        this.all = all;
        this.overriding = overriding;
        this.overridden = overridden;
        this.unique = unique;
        classified = List.<Classified>nul().join(overriding).join(overridden).join(unique);
    }

    MethodStruct cons(Overriding m) {
        return new MethodStruct(all, overriding.cons(m), overridden, unique);
    }

    MethodStruct cons(Overridden m) {
        return new MethodStruct(all, overriding, overridden.cons(m), unique);
    }

    MethodStruct cons(Unique m) {
        return new MethodStruct(all, overriding, overridden, unique.cons(m));
    }
}

abstract class Classified extends Method {
    Classified(Method m) {
        super(m.name, m.params, m.locals, m.retType, m.body, m.c);
    }

    abstract Function<Trans, Expr> call(Identifier self, List<Identifier> args);

    abstract Class origClass();
}

class Unique extends Classified {
    Unique(Method m) {
        super(m);
    }

    @Override
    Function<Trans, Expr> call(Identifier self, List<Identifier> args) {
        return Trans.genSym(res -> tr -> tr
                .cons(new Move_Id_FuncName(res, funcName()))
                .cons(new Call(res, res, args.toJavaList()))
                .applyTo(Expr.make(res, retType)));
    }

    @Override
    Class origClass() {
        return c;
    }
}

abstract class Virtual extends Classified {
    final Lazy<Integer> offset;

    Virtual(Method m) {
        super(m);
        this.offset = new Lazy<>(() -> origClass().vtables.head()
                .get().overrides.firstIndex(m::nameEquals).get() * 4);
    }

    @Override
    Function<Trans, Expr> call(Identifier self, List<Identifier> args) {
        return Trans.genSym(res -> tr -> tr
                .cons(new Load(res, self, origClass().ownObjOffset.get()))
                .cons(new Load(res, res, offset.get()))
                .cons(new Call(res, res, args.toJavaList()))
                .applyTo(Expr.make(res, retType)));
    }
}

class Overridden extends Virtual {
    Overridden(Method m) {
        super(m);
    }

    @Override
    public Class origClass() {
        return c;
    }
}

class Overriding extends Virtual {
    final Class target;

    Overriding(Method m, Class target) {
        super(m);
        this.target = target;
    }

    @Override
    public Class origClass() {
        return target;
    }
}

class Method extends Named {
    final List<Local> params;
    final List<Local> locals;
    final Optional<Class> retType;
    final MethodDeclaration body;
    final Class c;

    Method(String name,
            List<Local> params,
            List<Local> locals,
            Optional<Class> retType,
            MethodDeclaration body,
            Class c) {
        super(name);
        this.params = params;
        this.locals = locals;
        this.retType = retType;
        this.body = body;
        this.c = c;
    }

    FunctionDecl translate(TypeEnv typeEnv) {
        final var localsEnv = typeEnv.addLocals(params).addLocals(locals);
        final var trans = new Trans(List.nul(), 0).initLocals(locals);

        final var bodyEnv = body.f8.accept(new ListVisitor<>(new StmtVisitor()), localsEnv)
                .fold(trans, Trans::applyTo);

        final var ret = body.f10.accept(new ExprVisitor(), localsEnv).apply(bodyEnv);

        return new FunctionDecl(funcName(),
                params.map(s -> s.sym).cons(Trans.self).cons(Trans.stat).toJavaList(),
                new cs132.IR.sparrow.Block(ret.codeRev.reverse().toJavaList(), ret.sym));
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
    final Lazy<Integer> statSize;
    final Optional<Class> currClass;
    final List<Vtable> vtables;

    TypeEnv(List<Local> locals, List<Class> classes, Lazy<Integer> statSize, Optional<Class> currClass) {
        this.locals = locals;
        this.classes = classes;
        this.statSize = statSize;
        this.currClass = currClass;
        this.vtables = classes.flatMap(c -> c.vtables).unique(Object::equals);
    }

    TypeEnv enterClass(Class c) {
        return new TypeEnv(locals, classes, statSize, Optional.of(c));
    }

    Class classLookup(String name) {
        return classes.find(c -> c.name.equals(name)).get();
    }

    TypeEnv addLocals(List<Local> locals) {
        return new TypeEnv(this.locals.join(locals), classes, statSize, currClass);
    }

    Variable symLookup(String name) {
        return locals.find(s -> s.name.equals(name)).<Variable>map(x -> x)
                .or(() -> currClass.flatMap(c -> c.fieldLookup(name))).get();
    }
}

class Trans {
    final List<Instruction> codeRev;
    protected final int k;

    Trans(List<Instruction> codeRev, int k) {
        this.codeRev = codeRev;
        this.k = k;
    }

    static <T> Function<Trans, T> genSym(Function<Identifier, Function<Trans, T>> cont) {
        return tr -> cont.apply(new Identifier("v" + tr.k)).apply(new Trans(tr.codeRev, tr.k + 1));
    }

    static <T> Function<Trans, T> genLabel(Function<Label, Function<Trans, T>> cont) {
        return tr -> cont.apply(new Label("L" + tr.k)).apply(new Trans(tr.codeRev, tr.k + 1));
    }

    Trans cons(Instruction i) {
        return new Trans(codeRev.cons(i), k);
    }

    Trans initLocals(List<Local> locals) {
        return locals.fold(this, (acc, lc) -> acc.cons(new Move_Id_Integer(lc.sym, 0)));
    }

    static final Identifier self = new Identifier("this");

    static final Identifier stat = new Identifier("__stat__");

    <T> T applyTo(Function<Trans, T> f) {
        return f.apply(this);
    }
}

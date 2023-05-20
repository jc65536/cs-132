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
        super(tr, tr.k);
        this.sym = sym;
        this.type = type;
    }

    static Function<Trans, Expr> make(Identifier sym, Optional<Class> type) {
        return tr -> new Expr(tr, sym, type);
    }

    static final Function<Expr, Expr> nullCheck = e -> e
            .applyTo(Trans.genLabel(err -> Trans.genLabel(end -> tr -> tr
                    .cons(new IfGoto(e.sym, err))
                    .cons(new Goto(end))
                    .cons(new LabelInstr(err))
                    .cons(new ErrorMessage("\"null pointer\""))
                    .cons(new LabelInstr(end)))))
            .applyTo(Expr.make(e.sym, e.type));

    static Function<Expr, Expr> idxCheck(Identifier arr) {
        return e -> e.applyTo(Trans.genLabel(err -> Trans.genLabel(ok -> ExprVisitor.literal(0).andThen(tmp -> tmp
                .cons(new LessThan(tmp.sym, e.sym, tmp.sym))
                .cons(new IfGoto(tmp.sym, ok))
                .cons(new LabelInstr(err))
                .cons(new ErrorMessage("\"array index out of bounds\""))
                .cons(new LabelInstr(ok))
                .cons(new Load(tmp.sym, arr, 0))
                .cons(new LessThan(tmp.sym, e.sym, tmp.sym))
                .cons(new IfGoto(tmp.sym, err))))))
                .applyTo(Expr.make(e.sym, e.type));
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
    final Lazy<Integer> offset;

    Vtable(Class target, List<? extends Virtual> overrides, Lazy<Integer> offset) {
        this.target = target;
        this.overrides = overrides;
        this.size = overrides.count() * 4;
        this.offset = offset;
    }

    Trans write(Identifier tmp, Trans env) {
        return overrides.fold(env, (acc, m) -> acc
                .cons(new Move_Id_FuncName(tmp, m.funcName()))
                .cons(new Store(Trans.stat, offset.get() + m.offset.get(), tmp)));
    }
}

class Class extends Named {
    private final Optional<Lazy<Class>> superClass;
    final List<Field> fields;
    final MethodStruct methods;

    final List<Vtable> vtables;
    final Lazy<Integer> vtableEnd;

    final Lazy<Integer> objSize;
    final Lazy<Integer> ownObjOffset;

    Class(String name, Optional<Lazy<Class>> superClass,
            NodeListOptional fieldNodes, NodeListOptional methodNodes,
            Lazy<Integer> vtableOffset, Lazy<TypeEnv> env) {
        super(name);
        this.superClass = superClass;

        final var lazyFields = env.then(e -> mkFields(fieldNodes, e));
        fields = new List<>(lazyFields.bind(z -> z.a));
        final var fieldsSize = lazyFields.then(z -> z.b);

        final var lazyStruct = env.then(e -> mkMethods(methodNodes, e));
        final var struct = lazyStruct.bind(z -> z.b);
        methods = new MethodStruct(
                new List<>(lazyStruct.bind(z -> z.a)),
                new List<>(struct.bind(z -> z.overriding)),
                new List<>(struct.bind(z -> z.overridden)),
                new List<>(struct.bind(z -> z.unique)));

        final var lazyVtable = env.then(e -> mkVtables(vtableOffset, e));
        vtables = new List<>(lazyVtable.bind(z -> z.a));
        vtableEnd = lazyVtable.bind(z -> z.b);

        ownObjOffset = superClass.map(z -> z.bind(sc -> sc.objSize)).orElse(new Lazy<>(() -> 0));
        objSize = new Lazy<>(() -> ownObjOffset.get()
                + methods.overridden.head().map(u -> 4).orElse(0)
                + fieldsSize.get());
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
                                .cons(new Move_Id_Integer(tmp, vt.offset.get()))
                                .cons(new Add(tmp, Trans.stat, tmp))
                                .cons(new Store(obj, ownObjOffset.get(), tmp)))
                        .orElse(tr));
    }

    T2<List<Field>, Integer> mkFields(NodeListOptional fieldNodes, TypeEnv env) {
        final var fieldOffset = ownObjOffset.get() + methods.overridden.head().map(u -> 4).orElse(0);
        return fieldNodes.accept(new ListVisitor<>(new FieldVisitor()), env)
                .fold(new T2<>(List.<Field>nul(), fieldOffset), (acc, mkField) -> acc
                        .then(fields -> mkField.andThen(f -> new T2<>(fields.cons(f), f.offset + 4))));
    }

    T2<List<Vtable>, Lazy<Integer>> mkVtables(Lazy<Integer> vtableOffset, TypeEnv env) {
        return superClass().map(sc -> sc.vtables).orElse(List.nul())
                .fold(new T2<>(List.<Vtable>nul(), vtableOffset),
                        (acc, vt) -> acc.then(vtables -> offset -> {
                            final var overridingMethods = methods.overriding
                                    .filter(m -> m.origin() == vt.target);

                            return overridingMethods.head()
                                    .map(u -> vt.overrides.map(m -> overridingMethods
                                            .find(m::nameEquals).<Virtual>map(x -> x).orElse(m)))
                                    .map(overrides -> new Vtable(vt.target, overrides, offset))
                                    .map(nvt -> new T2<>(vtables.cons(nvt), offset.then(s -> s + vt.size)))
                                    .orElse(new T2<>(vtables.cons(vt), offset));
                        }))
                .then(vtables -> offset -> methods.overridden.head()
                        .map(u -> new Vtable(this, methods.overridden, offset))
                        .map(nvt -> new T2<>(vtables.cons(nvt), offset.then(s -> s + nvt.size)))
                        .orElse(new T2<>(vtables, offset)));
    }

    T2<List<Method>, Lazy<MethodStruct>> mkMethods(NodeListOptional methodNodes, TypeEnv env) {
        final var methods = methodNodes.accept(new ListVisitor<>(new MethodVisitor()), new T2<>(this, env));
        return new T2<>(methods, new Lazy<>(() -> methods.fold(
                new MethodStruct(methods, List.nul(), List.nul(), List.nul()),
                (struct, m) -> m.c.superClass()
                        .flatMap(sc -> sc.classifiedLookup(m.name))
                        .map(sm -> struct.cons(new Overriding(m, sm.origin())))
                        .or(() -> env.classes
                                .filter(cls -> cls != m.c && cls.subtypes(m.c))
                                .flatMap(cls -> cls.methods.all)
                                .find(m::nameEquals)
                                .map(u -> struct.cons(new Overridden(m))))
                        .orElseGet(() -> struct.cons(new Unique(m))))));
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
        return Trans.genSym(res -> tr -> tr
                .cons(new Load(res, Trans.self, offset))
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
        return Trans.genSym(res -> tr -> tr
                .cons(new Move_Id_Id(res, sym))
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

    MethodStruct(List<Method> all, List<Overriding> overriding, List<Overridden> overridden, List<Unique> unique) {
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

    abstract Class origin();
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
    Class origin() {
        return c;
    }
}

abstract class Virtual extends Classified {
    final Lazy<Integer> offset;

    Virtual(Method m) {
        super(m);
        this.offset = new Lazy<>(() -> origin().vtables.head()
                .get().overrides.firstIndex(m::nameEquals).get() * 4);
    }

    @Override
    Function<Trans, Expr> call(Identifier self, List<Identifier> args) {
        return Trans.genSym(res -> tr -> tr
                .cons(new Load(res, self, origin().ownObjOffset.get()))
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
    Class origin() {
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
    Class origin() {
        return target;
    }
}

class Method extends Named {
    final List<Local> params;
    final List<Local> locals;
    final Optional<Class> retType;
    final MethodDeclaration body;
    final Class c;

    Method(String name, List<Local> params, List<Local> locals,
            Optional<Class> retType, MethodDeclaration body, Class c) {
        super(name);
        this.params = params;
        this.locals = locals;
        this.retType = retType;
        this.body = body;
        this.c = c;
    }

    FunctionDecl translate(TypeEnv env) {
        final var localsEnv = env.addLocals(params).addLocals(locals);
        return new Trans(List.nul(), 0)
                .applyTo(tr -> tr.initLocals(locals))
                .applyTo(tr -> body.f8.accept(new ListVisitor<>(new StmtVisitor()), localsEnv)
                        .fold(tr, Trans::applyTo))
                .applyTo(body.f10.accept(new ExprVisitor(), localsEnv)
                        .andThen(ret -> new cs132.IR.sparrow.Block(ret.reverse().toJavaList(), ret.sym))
                        .andThen(block -> new FunctionDecl(funcName(),
                                params.map(s -> s.sym).cons(Trans.self).cons(Trans.stat).toJavaList(),
                                block)));
    }

    FunctionName funcName() {
        return new FunctionName(c.name + "_" + name);
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

    Variable varLookup(String name) {
        return locals.find(s -> s.name.equals(name)).<Variable>map(x -> x)
                .or(() -> currClass.flatMap(c -> c.fieldLookup(name))).get();
    }
}

class Trans extends List<Instruction> {
    protected final int k;

    static final Identifier self = new Identifier("this");

    static final Identifier stat = new Identifier("__stat__");

    Trans(List<Instruction> code, int k) {
        super(code);
        this.k = k;
    }

    static <T> Function<Trans, T> genSym(Function<Identifier, Function<Trans, T>> cont) {
        return tr -> cont.apply(new Identifier("v" + tr.k)).apply(new Trans(tr, tr.k + 1));
    }

    static <T> Function<Trans, T> genLabel(Function<Label, Function<Trans, T>> cont) {
        return tr -> cont.apply(new Label("L" + tr.k)).apply(new Trans(tr, tr.k + 1));
    }

    Trans cons(Instruction i) {
        return new Trans(super.cons(i), k);
    }

    Trans initLocals(List<Local> locals) {
        return locals.fold(this, (acc, lc) -> acc.cons(new Move_Id_Integer(lc.sym, 0)));
    }

    <T> T applyTo(Function<Trans, T> f) {
        return f.apply(this);
    }
}

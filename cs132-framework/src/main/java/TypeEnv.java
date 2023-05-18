import java.util.*;
import java.util.function.*;

import cs132.IR.sparrow.*;
import cs132.IR.token.*;
import cs132.IR.token.Identifier;
import cs132.minijava.syntaxtree.*;

class Expr {
    final Identifier sym;
    final Type type;
    final Translation tr;

    Expr(Identifier sym, Type type, Translation tr) {
        this.sym = sym;
        this.type = type;
        this.tr = tr;
    }

    Expr nullCheck() {
        return new Expr(sym, type, tr.applyTo(Translation.genLabel(err -> Translation.genLabel(end -> tr -> tr
                .cons(new IfGoto(sym, err))
                .cons(new Goto(end))
                .cons(new LabelInstr(err))
                .cons(new ErrorMessage("\"null pointer\""))
                .cons(new LabelInstr(end))))));
    }

    Expr idxCheck(Identifier arr) {
        return new Expr(sym, type,
                tr.applyTo(Translation.genSym(len -> Translation.genLabel(err -> Translation.genLabel(end -> tr -> tr
                        .cons(new Load(len, arr, 0))
                        .cons(new LessThan(len, sym, len))
                        .cons(new IfGoto(len, err))
                        .cons(new Goto(end))
                        .cons(new LabelInstr(err))
                        .cons(new ErrorMessage("\"array index out of bounds\""))
                        .cons(new LabelInstr(end)))))));
    }
}

interface Type {
    boolean subtypes(Type other);

    static final Type PRIM = new Type() {
        @Override
        public boolean subtypes(Type other) {
            return this == other;
        }
    };
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
    final List<? extends VtabledMethod> overrides;
    final int size;
    final int offset;

    Vtable(Class target, List<? extends VtabledMethod> overrides, int offset) {
        this.target = target;
        this.overrides = overrides;
        this.size = overrides.count() * 4;
        this.offset = offset;
    }

    Translation write(Identifier stat, Identifier tmp, Translation env) {
        env = env.cons(J2S.comment(String.format("Vtable_for_%s", target.name)));
        return overrides.fold(env, (acc, m) -> acc
                .cons(new Move_Id_FuncName(tmp, m.funcName()))
                .cons(new Store(stat, offset + m.offset.get(), tmp)));
    }
}

class Class extends Named implements Type {
    private final Optional<Lazy<Class>> superClass;
    final List<Field> fields;
    final MethodStruct methods;

    final List<Vtable> vtables;
    final Lazy<Integer> nextVtableOffset;

    // Size of entire object
    final Lazy<Integer> objSize;

    // Offset into object to own data
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

    @Override
    public boolean subtypes(Type other) {
        return this == other || superClass().map(sc -> sc.subtypes(other)).orElse(false);
    }

    Optional<Field> fieldLookup(String name) {
        return fields.find(s -> s.name.equals(name))
                .or(() -> superClass().flatMap(sc -> sc.fieldLookup(name)));
    }

    Optional<ClassifiedMethod> classifiedLookup(String name) {
        return methods.classified.find(m -> m.name.equals(name))
                .or(() -> superClass().flatMap(sc -> sc.classifiedLookup(name)));
    }

    Function<Translation, Expr> alloc() {
        return Translation.genSym(obj -> Translation.genSym(tmp -> tr -> new Expr(obj, this, tr
                .cons(new Move_Id_Integer(tmp, objSize.get()))
                .cons(new Alloc(obj, tmp))
                .applyTo(init(obj, tmp, vtables)))));
    }

    Function<Translation, Translation> init(Identifier obj, Identifier tmp, List<Vtable> vtables) {
        return superClass()
                .map(sc -> sc.init(obj, tmp, vtables))
                .orElse(x -> x)
                .andThen(tr -> vtables
                        .find(vt -> vt.target.equals(this))
                        .map(vt -> tr
                                .cons(new Move_Id_Integer(tmp, vt.offset))
                                .cons(new Add(tmp, Translation.stat, tmp))
                                .cons(new Store(obj, ownObjOffset.get(), tmp)))
                        .orElse(tr));
    }
}

abstract class Variable extends Named {
    final Type type;

    Variable(String name, Type type) {
        super(name);
        this.type = type;
    }

    abstract Function<Translation, Expr> toTemp();

    abstract Function<Translation, Translation> assign(Identifier src);
}

class Field extends Variable {
    final int offset;

    Field(String name, Type type, int offset) {
        super(name, type);
        this.offset = offset;
    }

    @Override
    Function<Translation, Expr> toTemp() {
        return Translation.genSym(tmp -> tr1 -> new Expr(tmp, type,
                tr1.cons(new Load(tmp, Translation.self, offset))));
    }

    @Override
    Function<Translation, Translation> assign(Identifier src) {
        return tr -> tr.cons(new Store(Translation.self, offset, src));
    }
}

class Local extends Variable {
    final Identifier sym;

    Local(String name, Type type, Identifier sym) {
        super(name, type);
        this.sym = sym;
    }

    @Override
    Function<Translation, Expr> toTemp() {
        return Translation.genSym(tmp -> tr1 -> new Expr(tmp, type,
                tr1.cons(new Move_Id_Id(tmp, sym))));
    }

    @Override
    Function<Translation, Translation> assign(Identifier src) {
        return tr -> tr.cons(new Move_Id_Id(sym, src));
    }
}

class MethodStruct {
    final List<Method> all;
    final List<OverridingMethod> overriding;
    final List<OverriddenMethod> overridden;
    final List<UniqueMethod> unique;
    final List<ClassifiedMethod> classified;

    MethodStruct(List<Method> all,
            List<OverridingMethod> overriding,
            List<OverriddenMethod> overridden,
            List<UniqueMethod> unique) {
        this.all = all;
        this.overriding = overriding;
        this.overridden = overridden;
        this.unique = unique;
        classified = List.<ClassifiedMethod>nul().join(overriding).join(overridden).join(unique);
    }

    MethodStruct cons(OverridingMethod m) {
        return new MethodStruct(all, overriding.cons(m), overridden, unique);
    }

    MethodStruct cons(OverriddenMethod m) {
        return new MethodStruct(all, overriding, overridden.cons(m), unique);
    }

    MethodStruct cons(UniqueMethod m) {
        return new MethodStruct(all, overriding, overridden, unique.cons(m));
    }
}

abstract class ClassifiedMethod extends Method {
    ClassifiedMethod(Method m) {
        super(m.name, m.params, m.locals, m.retType, m.body, m.c);
    }

    abstract Expr call(Identifier self, List<Identifier> args, Translation env);

    abstract Class origClass();
}

class UniqueMethod extends ClassifiedMethod {
    UniqueMethod(Method m) {
        super(m);
    }

    @Override
    Expr call(Identifier self, List<Identifier> args, Translation tr) {
        return Translation.genSym(res -> tr1 -> new Expr(res, retType, tr1
                .cons(new Move_Id_FuncName(res, funcName()))
                .cons(new Call(res, res, args.toJavaList()))))
                .apply(tr);
    }

    @Override
    Class origClass() {
        return c;
    }
}

abstract class VtabledMethod extends ClassifiedMethod {
    final Lazy<Integer> offset;

    VtabledMethod(Method m) {
        super(m);
        this.offset = new Lazy<>(() -> origClass().vtables.head()
                .get().overrides.firstIndex(m::nameEquals).get() * 4);
    }

    @Override
    Expr call(Identifier self, List<Identifier> args, Translation tr) {
        return Translation.genSym(res -> tr1 -> new Expr(res, retType, tr1
                .cons(new Load(res, self, origClass().ownObjOffset.get()))
                .cons(new Load(res, res, offset.get()))
                .cons(new Call(res, res, args.toJavaList()))))
                .apply(tr);
    }
}

class OverriddenMethod extends VtabledMethod {
    OverriddenMethod(Method m) {
        super(m);
    }

    @Override
    public Class origClass() {
        return c;
    }
}

class OverridingMethod extends VtabledMethod {
    final Class target;

    OverridingMethod(Method m, Class target) {
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
    final Type retType;
    final MethodDeclaration body;
    final Class c;

    Method(String name,
            List<Local> params,
            List<Local> locals,
            Type retType,
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
        final var tr = new Translation(List.nul(), 0).initLocals(locals);

        final var bodyEnv = body.f8.accept(new ListVisitor<>(new StmtVisitor()), localsEnv)
                .fold(tr, Translation::applyTo);

        final var ret = body.f10.accept(new ExprVisitor(), localsEnv).apply(bodyEnv);

        return new FunctionDecl(funcName(),
                params.map(s -> s.sym).cons(Translation.self).cons(Translation.stat).toJavaList(),
                new cs132.IR.sparrow.Block(ret.tr.codeRev.reverse().toJavaList(), ret.sym));
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
    final List<Vtable> vtables;

    TypeEnv(List<Local> locals, List<Class> classes, Optional<Class> currClass) {
        this.locals = locals;
        this.classes = classes;
        this.currClass = currClass;
        this.vtables = classes.flatMap(c -> c.vtables).unique(Object::equals);
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

    Variable symLookup(String name) {
        return locals.find(s -> s.name.equals(name)).<Variable>map(x -> x)
                .or(() -> currClass.flatMap(c -> c.fieldLookup(name))).get();
    }
}

class Translation {
    final List<Instruction> codeRev;
    private final int k;

    Translation(List<Instruction> code, int k) {
        this.codeRev = code;
        this.k = k;
    }

    static <T> Function<Translation, T> genSym(Function<Identifier, Function<Translation, T>> cont) {
        return tr -> cont.apply(new Identifier("v" + tr.k)).apply(new Translation(tr.codeRev, tr.k + 1));
    }

    static <T> Function<Translation, T> genLabel(Function<Label, Function<Translation, T>> cont) {
        return tr -> cont.apply(new Label("L" + tr.k)).apply(new Translation(tr.codeRev, tr.k + 1));
    }

    Translation cons(Instruction i) {
        return new Translation(codeRev.cons(i), k);
    }

    Translation initLocals(List<Local> locals) {
        return locals.fold(this, (acc, lc) -> acc.cons(new Move_Id_Integer(lc.sym, 0)));
    }

    static final Identifier self = new Identifier("this");

    static final Identifier stat = new Identifier("__stat__");

    <T> T applyTo(Function<Translation, T> f) {
        return f.apply(this);
    }
}

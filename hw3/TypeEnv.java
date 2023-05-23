import java.util.*;
import java.util.function.*;

import cs132.IR.sparrow.*;
import cs132.IR.token.*;
import cs132.IR.token.Identifier;
import cs132.minijava.syntaxtree.*;

// Utility

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

    // Used in transMain to write vtables to "static" memory
    Function<Trans, Trans> write(Identifier tmp) {
        return tr -> overrides.fold(tr, (acc, m) -> acc
                .cons(new Move_Id_FuncName(tmp, m.funcName()))
                .cons(new Store(Trans.stat, offset.get() + m.offset.get(), tmp)));
    }
}

// Many fields in Class are Lazy or Lists (which are also Lazy) because when
// first visiting the classes we really are only guaranteed to know its own
// name, since classes can be defined in any order. So after visiting all the
// classes and building the type environment, we can finally instantiate all
// those classes' associating data.

class Class extends Named {
    private final Optional<Lazy<Class>> superClass;
    final List<Field> fields;
    final MethodStruct methods;

    final List<Vtable> vtables;

    // Offset into static memory for the next class's vtables
    final Lazy<Integer> vtableEnd;

    // Size of entire object, including parent instances
    final Lazy<Integer> objSize;

    // Offset into entire object to the start of own object (optional vtable
    // pointer and any fields specific to this class)
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

    private Function<Trans, Trans> init(Identifier obj, Identifier tmp, List<Vtable> vtables) {
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

    private T2<List<Field>, Integer> mkFields(NodeListOptional fieldNodes, TypeEnv env) {
        final var fieldOffset = ownObjOffset.get() + methods.overridden.head().map(u -> 4).orElse(0);
        return fieldNodes.accept(new ListVisitor<>(new FieldVisitor()), env)
                .fold(new T2<>(List.<Field>nul(), fieldOffset), (acc, mkField) -> acc
                        .then(fields -> mkField.andThen(f -> new T2<>(fields.cons(f), f.offset + 4))));
    }

    private T2<List<Vtable>, Lazy<Integer>> mkVtables(Lazy<Integer> vtableOffset, TypeEnv env) {
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

    // For any method m:

    // If there exists a parent class that contains a method whose name clashes
    // with m, m is Overriding.

    // Else if there exists any class c in the type environment such that c
    // subclasses m's declaring class (m.c) and c contains a method whose name
    // clashes with m, m is Overridden.

    // Else, m is Unique.

    private T2<List<Method>, Lazy<MethodStruct>> mkMethods(NodeListOptional methodNodes, TypeEnv env) {
        final var methods = methodNodes.accept(new ListVisitor<>(new MethodVisitor()), new T2<>(this, env));
        return new T2<>(methods, new Lazy<>(() -> methods.fold(
                new MethodStruct(methods, List.nul(), List.nul(), List.nul()),
                (struct, m) -> m.c.superClass()
                        .flatMap(sc -> sc.classifiedLookup(m.name))
                        .map(sm -> struct.cons(new Overriding(m, sm.origin())))
                        .or(() -> env.classes
                                .filter(c -> c != m.c && c.subtypes(m.c))
                                .flatMap(c -> c.methods.all)
                                .find(m::nameEquals)
                                .map(u -> struct.cons(new Overridden(m))))
                        .orElseGet(() -> struct.cons(new Unique(m))))));
    }
}

// Fields and locals both support toTemp and assign operations, but fields use
// Load/Store while locals just use Move_Id_Id.

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

    // Important to make a new temp variable to prevent the local from being
    // overwritten.

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

// Utility

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

// Methods is just a collection of information. Methods are not useful except
// for comparing names.

class Method extends Named {
    final List<Local> params;
    final List<Local> locals;
    final Optional<Class> retType;
    final MethodDeclaration body;

    // Class in which this method was defined
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
                .applyTo(Trans.initLocals(locals))
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

// We can't call Methods because we don't know their override status.
// Classified methods are methods but we actually know whether they're
// overriding a parent method, overridden by a child, or unique, so we can call
// classified methods. See mkMethods for definition of overriding, overridden,
// and unique methods. Overriding and Overridden methods require a vtable
// lookup to call, while Unique methods can be called directly.

abstract class Classified extends Method {
    Classified(Method m) {
        super(m.name, m.params, m.locals, m.retType, m.body, m.c);
    }

    abstract Function<Trans, Expr> call(Identifier self, List<Identifier> args);

    // Refers to the class that will be responsible for hosting this method's
    // vtable, if any
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

// Both overridden and overriding methods need a vtable entry. Virtual is
// terminology borrowed from C++.

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
    // Refers to the parent class whom this method is overriding
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

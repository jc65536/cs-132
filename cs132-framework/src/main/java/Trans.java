import java.util.*;
import java.util.function.*;

import cs132.IR.sparrow.*;
import cs132.IR.token.*;
import cs132.IR.token.Identifier;

// Translation history is just a list of instructions with some extra methods
// to generate symbols

public class Trans extends List<Instruction> {
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

    static Function<Trans, Trans> initLocals(List<Local> locals) {
        return tr -> locals.fold(tr, (acc, lc) -> acc.cons(new Move_Id_Integer(lc.sym, 0)));
    }

    // Convenience method: instead of fun1.andThen(fun2).apply(tr) I can write
    // tr.applyTo(fun1.andThen(fun2)). Just makes things look more
    // chronological at the cost of extra parentheses.
    <T> T applyTo(Function<Trans, T> f) {
        return f.apply(this);
    }
}

// An expression evaluates to not only a translation but also a temp variable
// for the result and its type

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

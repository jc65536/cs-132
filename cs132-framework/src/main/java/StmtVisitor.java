import java.util.function.Function;

import cs132.IR.sparrow.*;
import cs132.minijava.syntaxtree.*;
import cs132.minijava.syntaxtree.Block;
import cs132.minijava.visitor.*;

public class StmtVisitor extends GJDepthFirst<Function<Trans, Trans>, TypeEnv> {
    @Override
    public Function<Trans, Trans> visit(Statement n, TypeEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Function<Trans, Trans> visit(Block n, TypeEnv argu) {
        return tr -> n.f1.accept(new ListVisitor<>(this), argu).fold(tr, Trans::applyTo);
    }

    @Override
    public Function<Trans, Trans> visit(AssignmentStatement n, TypeEnv argu) {
        return n.f2.accept(new ExprVisitor(), argu)
                .andThen(rhs -> rhs.applyTo(argu.symLookup(n.f0.f0.tokenImage).assign(rhs.sym)));
    }

    @Override
    public Function<Trans, Trans> visit(ArrayAssignmentStatement n, TypeEnv argu) {
        return n.f0.accept(new ExprVisitor(), argu)
                .andThen(Expr.nullCheck)
                .andThen(arr -> arr.applyTo(n.f2.accept(new ExprVisitor(), argu)
                        .andThen(Expr.idxCheck(arr.sym))
                        .andThen(idx -> idx.applyTo(n.f5.accept(new ExprVisitor(), argu)
                                .andThen(rhs -> rhs.applyTo(ExprVisitor.literal(4)
                                        .andThen(tmp -> tmp
                                                .cons(new Multiply(idx.sym, idx.sym, tmp.sym))
                                                .cons(new Add(arr.sym, arr.sym, idx.sym))
                                                .cons(new Store(arr.sym, 4, rhs.sym)))))))));
    }

    @Override
    public Function<Trans, Trans> visit(IfStatement n, TypeEnv argu) {
        return Trans.genLabel(end -> Trans.genLabel(fail -> n.f2
                .accept(new ExprVisitor(), argu)
                .andThen(cond -> cond.cons(new IfGoto(cond.sym, fail)))
                .andThen(n.f4.accept(this, argu))
                .andThen(ifStmt -> ifStmt.cons(new Goto(end)).cons(new LabelInstr(fail))))
                .andThen(n.f6.accept(this, argu))
                .andThen(elseStmt -> elseStmt.cons(new LabelInstr(end))));
    }

    @Override
    public Function<Trans, Trans> visit(WhileStatement n, TypeEnv argu) {
        return Trans.genLabel(start -> Trans.genLabel(end -> tr -> tr
                .cons(new LabelInstr(start))
                .applyTo(n.f2.accept(new ExprVisitor(), argu).andThen(cond -> cond
                        .cons(new IfGoto(cond.sym, end))))
                .applyTo(n.f4.accept(this, argu))
                .applyTo(body -> body.cons(new Goto(start)).cons(new LabelInstr(end)))));
    }

    @Override
    public Function<Trans, Trans> visit(PrintStatement n, TypeEnv argu) {
        return n.f2.accept(new ExprVisitor(), argu).andThen(arg -> arg.cons(new Print(arg.sym)));
    }
}

import java.util.function.Function;

import cs132.IR.sparrow.*;
import cs132.minijava.syntaxtree.*;
import cs132.minijava.syntaxtree.Block;
import cs132.minijava.visitor.*;

public class StmtVisitor extends GJDepthFirst<Function<TransEnv, TransEnv>, TypeEnv> {
    @Override
    public Function<TransEnv, TransEnv> visit(Statement n, TypeEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Function<TransEnv, TransEnv> visit(Block n, TypeEnv argu) {
        return tr -> n.f1.accept(new ListVisitor<>(this), argu)
                .fold(tr, (acc, mkTrans) -> mkTrans.apply(acc));
    }

    @Override
    public Function<TransEnv, TransEnv> visit(AssignmentStatement n, TypeEnv argu) {
        return n.f2.accept(new ExprVisitor(), argu)
                .andThen(rhs -> argu.symLookup(n.f0.f0.tokenImage).assign(rhs.sym, rhs.env));
    }

    @Override
    public Function<TransEnv, TransEnv> visit(ArrayAssignmentStatement n, TypeEnv argu) {
        return tr -> tr.genSym(tmp -> n.f0.accept(new ExprVisitor(), argu)
                .andThen(Expr::nullCheck)
                .andThen(arr -> n.f2.accept(new ExprVisitor(), argu)
                        .andThen(idx -> idx.idxCheck(arr.sym))
                        .andThen(idx -> n.f5.accept(new ExprVisitor(), argu)
                                .andThen(rhs -> rhs.env
                                        .cons(new Move_Id_Integer(tmp, 4))
                                        .cons(new Multiply(idx.sym, idx.sym, tmp))
                                        .cons(new Add(arr.sym, arr.sym, idx.sym))
                                        .cons(new Store(arr.sym, 4, rhs.sym)))
                                .apply(idx.env))
                        .apply(arr.env)));
    }

    @Override
    public Function<TransEnv, TransEnv> visit(IfStatement n, TypeEnv argu) {
        return tr -> tr.genLabel(fail -> tr1 -> tr1.genLabel(end -> n.f2
                .accept(new ExprVisitor(), argu)
                .andThen(cond -> cond.env.cons(new IfGoto(cond.sym, fail)))
                .andThen(n.f4.accept(this, argu))
                .andThen(ifStmt -> ifStmt
                        .cons(new Goto(end))
                        .cons(new LabelInstr(fail)))
                .andThen(n.f6.accept(this, argu))
                .andThen(elseStmt -> elseStmt.cons(new LabelInstr(end)))));
    }

    @Override
    public Function<TransEnv, TransEnv> visit(WhileStatement n, TypeEnv argu) {
        return tr -> tr.genLabel(start -> tr1 -> tr1.genLabel(end -> tr2 -> n.f2
                .accept(new ExprVisitor(), argu)
                .andThen(cond -> cond.env.cons(new IfGoto(cond.sym, end)))
                .andThen(n.f4.accept(this, argu))
                .andThen(body -> body
                        .cons(new Goto(start))
                        .cons(new LabelInstr(end)))
                .apply(tr2.cons(new LabelInstr(start)))));
    }

    @Override
    public Function<TransEnv, TransEnv> visit(PrintStatement n, TypeEnv argu) {
        return n.f2.accept(new ExprVisitor(), argu)
                .andThen(arg -> arg.env.cons(new Print(arg.sym)));
    }
}

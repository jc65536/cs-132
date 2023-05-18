import java.util.function.Function;

import cs132.IR.sparrow.*;
import cs132.minijava.syntaxtree.*;
import cs132.minijava.syntaxtree.Block;
import cs132.minijava.visitor.*;

public class StmtVisitor extends GJDepthFirst<Function<Translation, Translation>, TypeEnv> {
    @Override
    public Function<Translation, Translation> visit(Statement n, TypeEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Function<Translation, Translation> visit(Block n, TypeEnv argu) {
        return tr -> n.f1.accept(new ListVisitor<>(this), argu).fold(tr, Translation::applyTo);
    }

    @Override
    public Function<Translation, Translation> visit(AssignmentStatement n, TypeEnv argu) {
        return n.f2.accept(new ExprVisitor(), argu)
                .andThen(rhs -> rhs.tr.applyTo(argu.symLookup(n.f0.f0.tokenImage).assign(rhs.sym)));
    }

    @Override
    public Function<Translation, Translation> visit(ArrayAssignmentStatement n, TypeEnv argu) {
        return Translation.genSym(tmp -> n.f0.accept(new ExprVisitor(), argu)
                .andThen(Expr::nullCheck)
                .andThen(arr -> arr.tr.applyTo(n.f2.accept(new ExprVisitor(), argu)
                        .andThen(idx -> idx.idxCheck(arr.sym))
                        .andThen(idx -> idx.tr.applyTo(n.f5.accept(new ExprVisitor(), argu)
                                .andThen(rhs -> rhs.tr
                                        .cons(new Move_Id_Integer(tmp, 4))
                                        .cons(new Multiply(idx.sym, idx.sym, tmp))
                                        .cons(new Add(arr.sym, arr.sym, idx.sym))
                                        .cons(new Store(arr.sym, 4, rhs.sym))))))));
    }

    @Override
    public Function<Translation, Translation> visit(IfStatement n, TypeEnv argu) {
        return Translation.genLabel(fail -> Translation.genLabel(end -> n.f2
                .accept(new ExprVisitor(), argu)
                .andThen(cond -> cond.tr.cons(new IfGoto(cond.sym, fail)))
                .andThen(n.f4.accept(this, argu))
                .andThen(ifStmt -> ifStmt
                        .cons(new Goto(end))
                        .cons(new LabelInstr(fail)))
                .andThen(n.f6.accept(this, argu))
                .andThen(elseStmt -> elseStmt.cons(new LabelInstr(end)))));
    }

    @Override
    public Function<Translation, Translation> visit(WhileStatement n, TypeEnv argu) {
        return Translation.genLabel(start -> Translation.genLabel(end -> tr2 -> tr2
                .cons(new LabelInstr(start))
                .applyTo(n.f2.accept(new ExprVisitor(), argu)
                        .andThen(cond -> cond.tr.cons(new IfGoto(cond.sym, end)))
                        .andThen(n.f4.accept(this, argu))
                        .andThen(body -> body
                                .cons(new Goto(start))
                                .cons(new LabelInstr(end))))));
    }

    @Override
    public Function<Translation, Translation> visit(PrintStatement n, TypeEnv argu) {
        return n.f2.accept(new ExprVisitor(), argu)
                .andThen(arg -> arg.tr.cons(new Print(arg.sym)));
    }
}

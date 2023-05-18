import java.util.*;
import java.util.function.*;

import cs132.IR.sparrow.*;
import cs132.IR.token.Identifier;
import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

public class ExprVisitor extends GJDepthFirst<Function<Trans, Expr>, TypeEnv> {
    @Override
    public Function<Trans, Expr> visit(Expression n, TypeEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Function<Trans, Expr> visit(AndExpression n, TypeEnv argu) {
        return Trans.genSym(res -> Trans.genLabel(end -> n.f0.accept(this, argu)
                .andThen(lhs -> lhs
                        .cons(new Move_Id_Id(res, lhs.sym))
                        .cons(new IfGoto(res, end)))
                .andThen(n.f2.accept(this, argu))
                .andThen(rhs -> rhs
                        .cons(new Move_Id_Id(res, rhs.sym))
                        .cons(new LabelInstr(end)))
                .andThen(Expr.make(res, Optional.empty()))));
    }

    @Override
    public Function<Trans, Expr> visit(CompareExpression n, TypeEnv argu) {
        return binOp(n.f0, n.f2, argu, LessThan::new);
    }

    @Override
    public Function<Trans, Expr> visit(PlusExpression n, TypeEnv argu) {
        return binOp(n.f0, n.f2, argu, Add::new);
    }

    @Override
    public Function<Trans, Expr> visit(MinusExpression n, TypeEnv argu) {
        return binOp(n.f0, n.f2, argu, Subtract::new);
    }

    @Override
    public Function<Trans, Expr> visit(TimesExpression n, TypeEnv argu) {
        return binOp(n.f0, n.f2, argu, Multiply::new);
    }

    @Override
    public Function<Trans, Expr> visit(ArrayLookup n, TypeEnv argu) {
        return Trans.genSym(res -> n.f0.accept(this, argu)
                .andThen(arr -> arr.applyTo(n.f2.accept(this, argu)
                        .andThen(idx -> idx.idxCheck(arr.sym))
                        .andThen(idx -> idx
                                .cons(new Move_Id_Integer(res, 4))
                                .cons(new Multiply(idx.sym, idx.sym, res))
                                .cons(new Add(arr.sym, arr.sym, idx.sym))
                                .cons(new Load(res, arr.sym, 4)))
                        .andThen(Expr.make(res, Optional.empty())))));
    }

    @Override
    public Function<Trans, Expr> visit(ArrayLength n, TypeEnv argu) {
        return Trans.genSym(res -> n.f0.accept(this, argu)
                .andThen(Expr::nullCheck)
                .andThen(arr -> arr.cons(new Load(res, arr.sym, 0)))
                .andThen(Expr.make(res, Optional.empty())));
    }

    @Override
    public Function<Trans, Expr> visit(MessageSend n, TypeEnv argu) {
        return n.f0.accept(this, argu).andThen(obj -> n.f4
                .accept(new ListVisitor<>(new ExprVisitor()), argu)
                .fold(new T2<>(List.<Identifier>nul(), obj.nullCheck()),
                        (acc, mkExpr) -> acc.consume(list -> mkExpr
                                .andThen(arg -> new T2<>(list.cons(arg.sym), arg))))
                .<Expr>consume(args -> obj.type.get()
                        .classifiedLookup(n.f2.f0.tokenImage).get()
                        .call(obj.sym, args.reverse().cons(obj.sym).cons(Trans.stat))));
    }

    @Override
    public Function<Trans, Expr> visit(PrimaryExpression n, TypeEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Function<Trans, Expr> visit(IntegerLiteral n, TypeEnv argu) {
        return literal(Integer.parseInt(n.f0.tokenImage));
    }

    @Override
    public Function<Trans, Expr> visit(TrueLiteral n, TypeEnv argu) {
        return literal(1);
    }

    @Override
    public Function<Trans, Expr> visit(FalseLiteral n, TypeEnv argu) {
        return literal(0);
    }

    @Override
    public Function<Trans, Expr> visit(cs132.minijava.syntaxtree.Identifier n, TypeEnv argu) {
        return argu.symLookup(n.f0.tokenImage).toTemp();
    }

    @Override
    public Function<Trans, Expr> visit(ThisExpression n, TypeEnv argu) {
        return Trans.genSym(res -> tr -> tr.cons(new Move_Id_Id(res, Trans.self))
                .applyTo(Expr.make(res, argu.currClass)));
    }

    @Override
    public Function<Trans, Expr> visit(ArrayAllocationExpression n, TypeEnv argu) {
        return Trans.genSym(res -> Trans.genSym(size -> Trans.genLabel(ok -> n.f3.accept(this, argu)
                .andThen(len -> len
                        .cons(new Move_Id_Integer(res, 0))
                        .cons(new LessThan(res, len.sym, res))
                        .cons(new IfGoto(res, ok))
                        .cons(new ErrorMessage("\"array index out of bounds\""))
                        .cons(new LabelInstr(ok))
                        .cons(new Move_Id_Integer(res, 1))
                        .cons(new Add(size, len.sym, res))
                        .cons(new Move_Id_Integer(res, 4))
                        .cons(new Multiply(size, size, res))
                        .cons(new Alloc(res, size))
                        .cons(new Store(res, 0, len.sym)))
                .andThen(Expr.make(res, Optional.empty())))));
    }

    @Override
    public Function<Trans, Expr> visit(AllocationExpression n, TypeEnv argu) {
        return argu.classLookup(n.f1.f0.tokenImage).alloc();
    }

    @Override
    public Function<Trans, Expr> visit(NotExpression n, TypeEnv argu) {
        return Trans.genSym(res -> n.f1.accept(this, argu)
                .andThen(opd -> opd
                        .cons(new Move_Id_Integer(res, 1))
                        .cons(new Subtract(res, res, opd.sym)))
                .andThen(Expr.make(res, Optional.empty())));
    }

    @Override
    public Function<Trans, Expr> visit(BracketExpression n, TypeEnv argu) {
        return n.f1.accept(this, argu);
    }

    @Override
    public Function<Trans, Expr> visit(ExpressionRest n, TypeEnv argu) {
        return n.f1.accept(this, argu);
    }

    Function<Trans, Expr> binOp(Node lNode, Node rNode, TypeEnv argu,
            F3<Identifier, Identifier, Identifier, Instruction> mkInstr) {
        return Trans.genSym(res -> lNode.accept(this, argu)
                .andThen(lhs -> lhs.applyTo(rNode.accept(this, argu)
                        .andThen(rhs -> rhs.cons(mkInstr.apply(res, lhs.sym, rhs.sym)))
                        .andThen(Expr.make(res, Optional.empty())))));
    }

    Function<Trans, Expr> literal(int num) {
        return Trans.genSym(res -> tr -> tr.cons(new Move_Id_Integer(res, num))
                .applyTo(Expr.make(res, Optional.empty())));
    }
}

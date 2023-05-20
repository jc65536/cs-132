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
        return Trans.genLabel(end -> n.f0.accept(this, argu).andThen(lhs -> lhs
                .cons(new IfGoto(lhs.sym, end))
                .applyTo(n.f2.accept(this, argu).andThen(rhs -> rhs
                        .cons(new Move_Id_Id(lhs.sym, rhs.sym))
                        .cons(new LabelInstr(end)))
                        .andThen(Expr.make(lhs.sym, Optional.empty())))));
    }

    @Override
    public Function<Trans, Expr> visit(CompareExpression n, TypeEnv argu) {
        return binOp(n.f0, n.f2, argu, (lhs, rhs) -> new LessThan(lhs, lhs, rhs));
    }

    @Override
    public Function<Trans, Expr> visit(PlusExpression n, TypeEnv argu) {
        return binOp(n.f0, n.f2, argu, (lhs, rhs) -> new Add(lhs, lhs, rhs));
    }

    @Override
    public Function<Trans, Expr> visit(MinusExpression n, TypeEnv argu) {
        return binOp(n.f0, n.f2, argu, (lhs, rhs) -> new Subtract(lhs, lhs, rhs));
    }

    @Override
    public Function<Trans, Expr> visit(TimesExpression n, TypeEnv argu) {
        return binOp(n.f0, n.f2, argu, (lhs, rhs) -> new Multiply(lhs, lhs, rhs));
    }

    @Override
    public Function<Trans, Expr> visit(ArrayLookup n, TypeEnv argu) {
        return n.f0.accept(this, argu)
                .andThen(Expr.nullCheck)
                .andThen(arr -> arr.applyTo(n.f2.accept(this, argu)
                        .andThen(Expr.idxCheck(arr.sym))
                        .andThen(idx -> idx.applyTo(literal(4)
                                .andThen(tmp -> tmp
                                        .cons(new Multiply(idx.sym, idx.sym, tmp.sym))
                                        .cons(new Add(arr.sym, arr.sym, idx.sym))
                                        .cons(new Load(tmp.sym, arr.sym, 4))
                                        .applyTo(Expr.make(tmp.sym, Optional.empty())))))));
    }

    @Override
    public Function<Trans, Expr> visit(ArrayLength n, TypeEnv argu) {
        return n.f0.accept(this, argu)
                .andThen(Expr.nullCheck)
                .andThen(arr -> arr
                        .cons(new Load(arr.sym, arr.sym, 0))
                        .applyTo(Expr.make(arr.sym, Optional.empty())));
    }

    @Override
    public Function<Trans, Expr> visit(MessageSend n, TypeEnv argu) {
        return n.f0.accept(this, argu)
                .andThen(Expr.nullCheck)
                .andThen(obj -> n.f4.accept(new ListVisitor<>(new ExprVisitor()), argu)
                        .fold(new T2<>(List.<Identifier>nul(), obj), (acc, mkExpr) -> acc
                                .then(list -> mkExpr.andThen(arg -> new T2<>(list.cons(arg.sym), arg))))
                        .then(args -> obj.type.get().classifiedLookup(n.f2.f0.tokenImage).get()
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
        return Trans.genSym(tmp -> tr -> tr
                .cons(new Move_Id_Id(tmp, Trans.self))
                .applyTo(Expr.make(tmp, argu.currClass)));
    }

    @Override
    public Function<Trans, Expr> visit(ArrayAllocationExpression n, TypeEnv argu) {
        return Trans.genSym(size -> Trans.genLabel(ok -> n.f3.accept(this, argu).andThen(len -> len
                .applyTo(literal(0).andThen(tmp -> tmp
                        .cons(new LessThan(tmp.sym, len.sym, tmp.sym))
                        .cons(new IfGoto(tmp.sym, ok))
                        .cons(new ErrorMessage("\"array index out of bounds\""))
                        .cons(new LabelInstr(ok))
                        .cons(new Move_Id_Integer(tmp.sym, 1))
                        .cons(new Add(size, len.sym, tmp.sym))
                        .cons(new Move_Id_Integer(tmp.sym, 4))
                        .cons(new Multiply(size, size, tmp.sym))
                        .cons(new Alloc(tmp.sym, size))
                        .cons(new Store(tmp.sym, 0, len.sym))
                        .applyTo(Expr.make(tmp.sym, Optional.empty())))))));
    }

    @Override
    public Function<Trans, Expr> visit(AllocationExpression n, TypeEnv argu) {
        return argu.classLookup(n.f1.f0.tokenImage).alloc();
    }

    @Override
    public Function<Trans, Expr> visit(NotExpression n, TypeEnv argu) {
        return n.f1.accept(this, argu).andThen(opd -> opd
                .applyTo(literal(1).andThen(tmp -> tmp
                        .cons(new Subtract(tmp.sym, tmp.sym, opd.sym))
                        .applyTo(Expr.make(tmp.sym, Optional.empty())))));

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
            BiFunction<Identifier, Identifier, Instruction> mkInstr) {
        return lNode.accept(this, argu).andThen(lhs -> lhs
                .applyTo(rNode.accept(this, argu).andThen(rhs -> rhs
                        .cons(mkInstr.apply(lhs.sym, rhs.sym)))
                        .andThen(Expr.make(lhs.sym, Optional.empty()))));
    }

    static Function<Trans, Expr> literal(int num) {
        return Trans.genSym(res -> tr -> tr
                .cons(new Move_Id_Integer(res, num))
                .applyTo(Expr.make(res, Optional.empty())));
    }
}

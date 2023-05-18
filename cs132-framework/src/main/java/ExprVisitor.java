import java.util.function.*;

import cs132.IR.sparrow.*;
import cs132.IR.token.Identifier;
import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

public class ExprVisitor extends GJDepthFirst<Function<TransEnv, Expr>, TypeEnv> {
    @Override
    public Function<TransEnv, Expr> visit(Expression n, TypeEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Function<TransEnv, Expr> visit(AndExpression n, TypeEnv argu) {
        return tr -> tr.genSym(res -> tr1 -> tr1.genLabel(end -> n.f0.accept(this, argu)
                .andThen(lhs -> lhs.env
                        .cons(new Move_Id_Id(res, lhs.sym))
                        .cons(new IfGoto(res, end)))
                .andThen(n.f2.accept(this, argu))
                .andThen(rhs -> rhs.env
                        .cons(new Move_Id_Id(res, rhs.sym))
                        .cons(new LabelInstr(end)))
                .andThen(env -> new Expr(res, Type.PRIM, env))));
    }

    @Override
    public Function<TransEnv, Expr> visit(CompareExpression n, TypeEnv argu) {
        return binOp(n.f0, n.f2, argu, LessThan::new);
    }

    @Override
    public Function<TransEnv, Expr> visit(PlusExpression n, TypeEnv argu) {
        return binOp(n.f0, n.f2, argu, Add::new);
    }

    @Override
    public Function<TransEnv, Expr> visit(MinusExpression n, TypeEnv argu) {
        return binOp(n.f0, n.f2, argu, Subtract::new);
    }

    @Override
    public Function<TransEnv, Expr> visit(TimesExpression n, TypeEnv argu) {
        return binOp(n.f0, n.f2, argu, Multiply::new);
    }

    @Override
    public Function<TransEnv, Expr> visit(ArrayLookup n, TypeEnv argu) {
        return tr -> tr.genSym(res -> n.f0.accept(this, argu)
                .andThen(arr -> n.f2.accept(this, argu)
                        .andThen(idx -> idx.idxCheck(arr.sym))
                        .andThen(idx -> idx.env
                                .cons(new Move_Id_Integer(res, 4))
                                .cons(new Multiply(idx.sym, idx.sym, res))
                                .cons(new Add(arr.sym, arr.sym, idx.sym))
                                .cons(new Load(res, arr.sym, 4)))
                        .andThen(env -> new Expr(res, Type.PRIM, env))
                        .apply(arr.env)));
    }

    @Override
    public Function<TransEnv, Expr> visit(ArrayLength n, TypeEnv argu) {
        return tr -> tr.genSym(res -> n.f0.accept(this, argu)
                .andThen(Expr::nullCheck)
                .andThen(arr -> new Expr(res, Type.PRIM,
                        arr.env.cons(new Load(res, arr.sym, 0)))));
    }

    @Override
    public Function<TransEnv, Expr> visit(MessageSend n, TypeEnv argu) {
        return n.f0.accept(this, argu).andThen(obj -> n.f4
                .accept(new ListVisitor<>(new ExprVisitor()), argu)
                .fold(new T2<>(obj.nullCheck().env, List.<Identifier>nul()),
                        (acc, mkExpr) -> acc.consume((tr, list) -> mkExpr
                                .andThen(arg -> new T2<>(arg.env, list.cons(arg.sym))).apply(tr)))
                .consume((evalArgs, args) -> ((Class) obj.type)
                        .classifiedLookup(n.f2.f0.tokenImage).get()
                        .call(obj.sym, args.reverse().cons(obj.sym).cons(TransEnv.stat), evalArgs)));
    }

    @Override
    public Function<TransEnv, Expr> visit(PrimaryExpression n, TypeEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Function<TransEnv, Expr> visit(IntegerLiteral n, TypeEnv argu) {
        return literal(Integer.parseInt(n.f0.tokenImage));
    }

    @Override
    public Function<TransEnv, Expr> visit(TrueLiteral n, TypeEnv argu) {
        return literal(1);
    }

    @Override
    public Function<TransEnv, Expr> visit(FalseLiteral n, TypeEnv argu) {
        return literal(0);
    }

    @Override
    public Function<TransEnv, Expr> visit(cs132.minijava.syntaxtree.Identifier n, TypeEnv argu) {
        return argu.symLookup(n.f0.tokenImage)::toTemp;
    }

    @Override
    public Function<TransEnv, Expr> visit(ThisExpression n, TypeEnv argu) {
        return tr -> tr.genSym(tmp -> tr1 -> new Expr(tmp, argu.currClass.get(),
                tr1.cons(new Move_Id_Id(tmp, TransEnv.self))));
    }

    @Override
    public Function<TransEnv, Expr> visit(ArrayAllocationExpression n, TypeEnv argu) {
        return tr -> tr.genSym(res -> tr1 -> tr1.genSym(size -> tr2 -> tr2.genLabel(ok -> n.f3.accept(this, argu)
                .andThen(len -> len.env
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
                .andThen(env -> new Expr(res, Type.PRIM, env)))));
    }

    @Override
    public Function<TransEnv, Expr> visit(AllocationExpression n, TypeEnv argu) {
        return argu.classLookup(n.f1.f0.tokenImage)::alloc;
    }

    @Override
    public Function<TransEnv, Expr> visit(NotExpression n, TypeEnv argu) {
        return tr -> tr.genSym(res -> n.f1.accept(this, argu)
                .andThen(opd -> opd.env
                        .cons(new Move_Id_Integer(res, 1))
                        .cons(new Subtract(res, res, opd.sym)))
                .andThen(env -> new Expr(res, Type.PRIM, env)));
    }

    @Override
    public Function<TransEnv, Expr> visit(BracketExpression n, TypeEnv argu) {
        return n.f1.accept(this, argu);
    }

    @Override
    public Function<TransEnv, Expr> visit(ExpressionRest n, TypeEnv argu) {
        return n.f1.accept(this, argu);
    }

    Function<TransEnv, Expr> binOp(Node lNode, Node rNode, TypeEnv argu,
            F3<Identifier, Identifier, Identifier, Instruction> mkInstr) {
        return tr -> tr.genSym(res -> lNode.accept(this, argu)
                .andThen(lhs -> rNode.accept(this, argu)
                        .andThen(rhs -> rhs.env.cons(mkInstr.apply(res, lhs.sym, rhs.sym)))
                        .andThen(env -> new Expr(res, Type.PRIM, env))
                        .apply(lhs.env)));
    }

    Function<TransEnv, Expr> literal(int num) {
        return tr -> tr.genSym((res, tr1) -> new Expr(res, Type.PRIM,
                tr1.cons(new Move_Id_Integer(res, num))));
    }
}

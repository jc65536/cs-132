import java.util.function.*;

import cs132.IR.sparrow.*;
import cs132.IR.token.Identifier;
import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

public class ExprVisitor extends GJDepthFirst<Function<Translation, Expr>, TypeEnv> {
    @Override
    public Function<Translation, Expr> visit(Expression n, TypeEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Function<Translation, Expr> visit(AndExpression n, TypeEnv argu) {
        return Translation.genSym(res -> Translation.genLabel(end -> n.f0.accept(this, argu)
                .andThen(lhs -> lhs.tr
                        .cons(new Move_Id_Id(res, lhs.sym))
                        .cons(new IfGoto(res, end)))
                .andThen(n.f2.accept(this, argu))
                .andThen(rhs -> rhs.tr
                        .cons(new Move_Id_Id(res, rhs.sym))
                        .cons(new LabelInstr(end)))
                .andThen(env -> new Expr(res, Type.PRIM, env))));
    }

    @Override
    public Function<Translation, Expr> visit(CompareExpression n, TypeEnv argu) {
        return binOp(n.f0, n.f2, argu, LessThan::new);
    }

    @Override
    public Function<Translation, Expr> visit(PlusExpression n, TypeEnv argu) {
        return binOp(n.f0, n.f2, argu, Add::new);
    }

    @Override
    public Function<Translation, Expr> visit(MinusExpression n, TypeEnv argu) {
        return binOp(n.f0, n.f2, argu, Subtract::new);
    }

    @Override
    public Function<Translation, Expr> visit(TimesExpression n, TypeEnv argu) {
        return binOp(n.f0, n.f2, argu, Multiply::new);
    }

    @Override
    public Function<Translation, Expr> visit(ArrayLookup n, TypeEnv argu) {
        return Translation.genSym(res -> n.f0.accept(this, argu)
                .andThen(arr -> arr.tr.applyTo(n.f2.accept(this, argu)
                        .andThen(idx -> idx.idxCheck(arr.sym))
                        .andThen(idx -> idx.tr
                                .cons(new Move_Id_Integer(res, 4))
                                .cons(new Multiply(idx.sym, idx.sym, res))
                                .cons(new Add(arr.sym, arr.sym, idx.sym))
                                .cons(new Load(res, arr.sym, 4)))
                        .andThen(env -> new Expr(res, Type.PRIM, env)))));
    }

    @Override
    public Function<Translation, Expr> visit(ArrayLength n, TypeEnv argu) {
        return Translation.genSym(res -> n.f0.accept(this, argu)
                .andThen(Expr::nullCheck)
                .andThen(arr -> new Expr(res, Type.PRIM,
                        arr.tr.cons(new Load(res, arr.sym, 0)))));
    }

    @Override
    public Function<Translation, Expr> visit(MessageSend n, TypeEnv argu) {
        return n.f0.accept(this, argu).andThen(obj -> n.f4
                .accept(new ListVisitor<>(new ExprVisitor()), argu)
                .fold(new T2<>(obj.nullCheck().tr, List.<Identifier>nul()),
                        (acc, mkExpr) -> acc.consume((tr, list) -> tr.applyTo(mkExpr
                                .andThen(arg -> new T2<>(arg.tr, list.cons(arg.sym))))))
                .consume((evalArgs, args) -> ((Class) obj.type)
                        .classifiedLookup(n.f2.f0.tokenImage).get()
                        .call(obj.sym, args.reverse().cons(obj.sym).cons(Translation.stat), evalArgs)));
    }

    @Override
    public Function<Translation, Expr> visit(PrimaryExpression n, TypeEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Function<Translation, Expr> visit(IntegerLiteral n, TypeEnv argu) {
        return literal(Integer.parseInt(n.f0.tokenImage));
    }

    @Override
    public Function<Translation, Expr> visit(TrueLiteral n, TypeEnv argu) {
        return literal(1);
    }

    @Override
    public Function<Translation, Expr> visit(FalseLiteral n, TypeEnv argu) {
        return literal(0);
    }

    @Override
    public Function<Translation, Expr> visit(cs132.minijava.syntaxtree.Identifier n, TypeEnv argu) {
        return argu.symLookup(n.f0.tokenImage).toTemp();
    }

    @Override
    public Function<Translation, Expr> visit(ThisExpression n, TypeEnv argu) {
        return Translation.genSym(tmp -> tr -> new Expr(tmp, argu.currClass.get(),
                tr.cons(new Move_Id_Id(tmp, Translation.self))));
    }

    @Override
    public Function<Translation, Expr> visit(ArrayAllocationExpression n, TypeEnv argu) {
        return Translation.genSym(res -> Translation.genSym(size -> Translation.genLabel(ok -> n.f3.accept(this, argu)
                .andThen(len -> len.tr
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
    public Function<Translation, Expr> visit(AllocationExpression n, TypeEnv argu) {
        return argu.classLookup(n.f1.f0.tokenImage).alloc();
    }

    @Override
    public Function<Translation, Expr> visit(NotExpression n, TypeEnv argu) {
        return Translation.genSym(res -> n.f1.accept(this, argu)
                .andThen(opd -> opd.tr
                        .cons(new Move_Id_Integer(res, 1))
                        .cons(new Subtract(res, res, opd.sym)))
                .andThen(env -> new Expr(res, Type.PRIM, env)));
    }

    @Override
    public Function<Translation, Expr> visit(BracketExpression n, TypeEnv argu) {
        return n.f1.accept(this, argu);
    }

    @Override
    public Function<Translation, Expr> visit(ExpressionRest n, TypeEnv argu) {
        return n.f1.accept(this, argu);
    }

    Function<Translation, Expr> binOp(Node lNode, Node rNode, TypeEnv argu,
            F3<Identifier, Identifier, Identifier, Instruction> mkInstr) {
        return Translation.genSym(res -> lNode.accept(this, argu)
                .andThen(lhs -> lhs.tr.applyTo(rNode.accept(this, argu)
                        .andThen(rhs -> rhs.tr.cons(mkInstr.apply(res, lhs.sym, rhs.sym)))
                        .andThen(env -> new Expr(res, Type.PRIM, env)))));
    }

    Function<Translation, Expr> literal(int num) {
        return Translation.genSym(res -> tr -> new Expr(res, Type.PRIM,
                tr.cons(new Move_Id_Integer(res, num))));
    }
}

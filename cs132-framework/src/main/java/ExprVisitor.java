import cs132.IR.sparrow.*;
import cs132.IR.token.Identifier;
import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

class Expr {
    final Identifier sym;
    final Type type;
    final TransEnv env;

    Expr(Identifier sym, Type type, TransEnv env) {
        this.sym = sym;
        this.type = type;
        this.env = env;
    }
}

public class ExprVisitor extends GJDepthFirst<Expr, T2<TypeEnv, TransEnv>> {
    @Override
    public Expr visit(Expression n, T2<TypeEnv, TransEnv> argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Expr visit(AndExpression n, T2<TypeEnv, TransEnv> argu) {
        return argu.b.genSym((res, env1) -> env1.genLabel((end, env2) -> {
            final var lhs = n.f0.accept(this, argu.setB(env2));
            final var rhs = n.f2.accept(this, argu.setB(lhs.env
                    .cons(new Move_Id_Id(res, lhs.sym))
                    .cons(new IfGoto(res, end))));
            return new Expr(res, Type.PRIM, rhs.env
                    .cons(new Move_Id_Id(res, rhs.sym))
                    .cons(new LabelInstr(end)));
        }));
    }

    @Override
    public Expr visit(CompareExpression n, T2<TypeEnv, TransEnv> argu) {
        return binOp(n.f0, n.f2, argu, LessThan::new);
    }

    @Override
    public Expr visit(PlusExpression n, T2<TypeEnv, TransEnv> argu) {
        return binOp(n.f0, n.f2, argu, Add::new);
    }

    @Override
    public Expr visit(MinusExpression n, T2<TypeEnv, TransEnv> argu) {
        return binOp(n.f0, n.f2, argu, Subtract::new);
    }

    @Override
    public Expr visit(TimesExpression n, T2<TypeEnv, TransEnv> argu) {
        return binOp(n.f0, n.f2, argu, Multiply::new);
    }

    @Override
    public Expr visit(ArrayLookup n, T2<TypeEnv, TransEnv> argu) {
        return argu.b.genSym((res, env) -> {
            final var arr = n.f0.accept(this, argu.setB(env));
            final var idx = n.f2.accept(this, argu.setB(arr.env.nullCheck(arr.sym)));
            return new Expr(res, Type.PRIM, idx.env.idxCheck(arr.sym, idx.sym)
                    .cons(new Move_Id_Integer(res, 4))
                    .cons(new Multiply(idx.sym, idx.sym, res))
                    .cons(new Add(arr.sym, arr.sym, idx.sym))
                    .cons(new Load(res, arr.sym, 4)));
        });
    }

    @Override
    public Expr visit(ArrayLength n, T2<TypeEnv, TransEnv> argu) {
        return argu.b.genSym((res, env) -> {
            final var arr = n.f0.accept(this, argu.setB(env));
            return new Expr(res, Type.PRIM, arr.env.nullCheck(arr.sym)
                    .cons(new Load(res, arr.sym, 0)));
        });
    }

    @Override
    public Expr visit(MessageSend n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var obj = n.f0.accept(this, argu);
        final var objClass = (Class) obj.type;

        return n.f4.accept(new FoldVisitor<>(new ExprVisitor(),
                (acc, exprRet) -> new T3<>(typeEnv, exprRet.env, acc.c.cons(exprRet.sym))),
                new T3<>(typeEnv, obj.env.nullCheck(obj.sym), List.<Identifier>nul()))
                .consume((u, evalArgs, args) -> objClass
                        .classifiedLookup(n.f2.f0.tokenImage).get()
                        .call(obj.sym, args, evalArgs));
    }

    @Override
    public Expr visit(PrimaryExpression n, T2<TypeEnv, TransEnv> argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Expr visit(IntegerLiteral n, T2<TypeEnv, TransEnv> argu) {
        return literal(Integer.parseInt(n.f0.tokenImage), argu.b);
    }

    @Override
    public Expr visit(TrueLiteral n, T2<TypeEnv, TransEnv> argu) {
        return literal(1, argu.b);
    }

    @Override
    public Expr visit(FalseLiteral n, T2<TypeEnv, TransEnv> argu) {
        return literal(0, argu.b);
    }

    @Override
    public Expr visit(cs132.minijava.syntaxtree.Identifier n, T2<TypeEnv, TransEnv> argu) {
        return argu.a.symLookup(n.f0.tokenImage).toTemp(argu.b);
    }

    @Override
    public Expr visit(ThisExpression n, T2<TypeEnv, TransEnv> argu) {
        return new Expr(TransEnv.self, argu.a.currClass.get(), argu.b);
    }

    @Override
    public Expr visit(ArrayAllocationExpression n, T2<TypeEnv, TransEnv> argu) {
        return argu.b.genSym((res, env1) -> env1.genSym((size, env2) -> env2.genLabel((ok, env3) -> {
            final var len = n.f3.accept(this, argu.setB(env3));
            return new Expr(res, Type.PRIM, len.env
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
                    .cons(new Store(res, 0, len.sym)));
        })));
    }

    @Override
    public Expr visit(AllocationExpression n, T2<TypeEnv, TransEnv> argu) {
        return argu.a.classLookup(n.f1.f0.tokenImage).alloc(argu.b);
    }

    @Override
    public Expr visit(NotExpression n, T2<TypeEnv, TransEnv> argu) {
        return argu.b.genSym((res, env) -> {
            final var opd = n.f1.accept(this, argu.setB(env));
            return new Expr(res, Type.PRIM, opd.env
                    .cons(new Move_Id_Integer(res, 1))
                    .cons(new Subtract(res, res, opd.sym)));
        });
    }

    @Override
    public Expr visit(BracketExpression n, T2<TypeEnv, TransEnv> argu) {
        return n.f1.accept(this, argu);
    }

    @Override
    public Expr visit(ExpressionRest n, T2<TypeEnv, TransEnv> argu) {
        return n.f1.accept(this, argu);
    }

    Expr binOp(Node lNode, Node rNode, T2<TypeEnv, TransEnv> argu,
            F3<Identifier, Identifier, Identifier, Instruction> mkInstr) {
        return argu.b.genSym((res, env) -> {
            final var lhs = lNode.accept(this, argu.setB(env));
            final var rhs = rNode.accept(this, argu.setB(lhs.env));
            return new Expr(res, Type.PRIM, rhs.env.cons(mkInstr.apply(res, lhs.sym, rhs.sym)));
        });
    }

    Expr literal(int num, TransEnv env) {
        return env.genSym((res, env1) -> new Expr(res, Type.PRIM,
                env1.cons(new Move_Id_Integer(res, num))));
    }
}

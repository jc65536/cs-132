import cs132.IR.sparrow.*;
import cs132.IR.token.Identifier;
import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

public class ExprVisitor extends GJDepthFirst<ExprVisitor.Ret, T2<TypeEnv, TransEnv>> {
    static class Ret {
        final Identifier sym;
        final Type type;
        final TransEnv env;

        Ret(Identifier sym, Type type, TransEnv env) {
            this.sym = sym;
            this.type = type;
            this.env = env;
        }
    }

    @Override
    public ExprVisitor.Ret visit(Expression n, T2<TypeEnv, TransEnv> argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public ExprVisitor.Ret visit(AndExpression n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        return transEnv.genSym((res, env1) -> env1.genLabel((end, env2) -> {
            final var lhs = n.f0.accept(this, new T2<>(typeEnv, env2));
            final var rhs = n.f2.accept(this, new T2<>(typeEnv, lhs.env
                    .cons(new Move_Id_Id(res, lhs.sym))
                    .cons(new IfGoto(lhs.sym, end))));
            return new Ret(res, Prim.PRIMITIVE, rhs.env
                    .cons(new Move_Id_Id(res, rhs.sym))
                    .cons(new LabelInstr(end)));
        }));
    }

    @Override
    public ExprVisitor.Ret visit(CompareExpression n, T2<TypeEnv, TransEnv> argu) {
        return binOp(n.f0, n.f2, argu, LessThan::new);
    }

    @Override
    public ExprVisitor.Ret visit(PlusExpression n, T2<TypeEnv, TransEnv> argu) {
        return binOp(n.f0, n.f2, argu, Add::new);
    }

    @Override
    public ExprVisitor.Ret visit(MinusExpression n, T2<TypeEnv, TransEnv> argu) {
        return binOp(n.f0, n.f2, argu, Subtract::new);
    }

    @Override
    public ExprVisitor.Ret visit(TimesExpression n, T2<TypeEnv, TransEnv> argu) {
        return binOp(n.f0, n.f2, argu, Multiply::new);
    }

    @Override
    public ExprVisitor.Ret visit(ArrayLookup n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        return transEnv.genSym((res, env) -> {
            final var arr = n.f0.accept(this, new T2<>(typeEnv, env));
            final var idx = n.f2.accept(this, new T2<>(typeEnv, arr.env.nullCheck(arr.sym)));
            return new Ret(res, Prim.PRIMITIVE, idx.env
                    .idxCheck(arr.sym, idx.sym)
                    .cons(new Move_Id_Integer(res, 4))
                    .cons(new Multiply(idx.sym, idx.sym, res))
                    .cons(new Add(arr.sym, arr.sym, idx.sym))
                    .cons(new Load(res, arr.sym, 4)));
        });

    }

    @Override
    public ExprVisitor.Ret visit(ArrayLength n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        return transEnv.genSym((res, env) -> {
            final var arr = n.f0.accept(this, new T2<>(typeEnv, env));
            return new Ret(res, Prim.PRIMITIVE, arr.env
                    .nullCheck(arr.sym)
                    .cons(new Load(res, arr.sym, 0)));
        });
    }

    @Override
    public ExprVisitor.Ret visit(MessageSend n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var obj = n.f0.accept(this, argu);
        final var objClass = (Class) obj.type;

        final var t1 = n.f4.accept(new FoldVisitor<>(new ExprVisitor(),
                (acc, exprRet) -> new T3<>(typeEnv, exprRet.env, acc.c.cons(exprRet.sym))),
                new T3<>(typeEnv, obj.env.nullCheck(obj.sym), List.<Identifier>nul()));

        final var args = t1.c.reverse().cons(obj.sym).cons(TransEnv.stat);
        final var argsEnv = t1.b;

        final var m = objClass.methodLookup(n.f2.f0.tokenImage).get();

        final var t2 = m.status.get().call(obj.sym, args, argsEnv);

        return new Ret(t2.a, m.retType, t2.b);
    }

    @Override
    public ExprVisitor.Ret visit(PrimaryExpression n, T2<TypeEnv, TransEnv> argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public ExprVisitor.Ret visit(IntegerLiteral n, T2<TypeEnv, TransEnv> argu) {
        return literal(Integer.parseInt(n.f0.tokenImage), argu);
    }

    @Override
    public ExprVisitor.Ret visit(TrueLiteral n, T2<TypeEnv, TransEnv> argu) {
        return literal(1, argu);
    }

    @Override
    public ExprVisitor.Ret visit(FalseLiteral n, T2<TypeEnv, TransEnv> argu) {
        return literal(0, argu);
    }

    @Override
    public ExprVisitor.Ret visit(cs132.minijava.syntaxtree.Identifier n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        final var v = typeEnv.symLookup(n.f0.tokenImage);

        final var t = v.toTemp(transEnv);
        final var sym = t.a;
        final var env = t.b;

        return new Ret(sym, v.type, env);
    }

    @Override
    public ExprVisitor.Ret visit(ThisExpression n, T2<TypeEnv, TransEnv> argu) {
        return new Ret(TransEnv.self, argu.a.currClass.get(), argu.b);
    }

    @Override
    public ExprVisitor.Ret visit(ArrayAllocationExpression n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        return transEnv.genSym((res, env1) -> env1.genSym((size, env2) -> env2.genLabel((ok, env3) -> {
            final var len = n.f3.accept(this, new T2<>(typeEnv, env3));
            return new Ret(res, Prim.PRIMITIVE, len.env
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
    public ExprVisitor.Ret visit(AllocationExpression n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        final var className = n.f1.f0.tokenImage;
        final var cls = typeEnv.classLookup(className);
        final var t = cls.alloc(transEnv);
        return new Ret(t.a, cls, t.b);
    }

    @Override
    public ExprVisitor.Ret visit(NotExpression n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        return transEnv.genSym((res, env) -> {
            final var opd = n.f1.accept(this, new T2<>(typeEnv, env));
            return new Ret(res, Prim.PRIMITIVE, opd.env
                    .cons(new Move_Id_Integer(res, 1))
                    .cons(new Subtract(res, res, opd.sym)));
        });
    }

    @Override
    public ExprVisitor.Ret visit(BracketExpression n, T2<TypeEnv, TransEnv> argu) {
        return n.f1.accept(this, argu);
    }

    @Override
    public ExprVisitor.Ret visit(ExpressionRest n, T2<TypeEnv, TransEnv> argu) {
        return n.f1.accept(this, argu);
    }

    Ret binOp(Node lNode, Node rNode, T2<TypeEnv, TransEnv> argu,
            F3<Identifier, Identifier, Identifier, Instruction> mkInstr) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        return transEnv.genSym((res, env) -> {
            final var lhs = lNode.accept(this, argu);
            final var rhs = rNode.accept(this, new T2<>(typeEnv, lhs.env));
            return new Ret(res, Prim.PRIMITIVE, rhs.env.cons(mkInstr.apply(res, lhs.sym, rhs.sym)));
        });
    }

    Ret literal(int num, T2<TypeEnv, TransEnv> argu) {
        return argu.b.genSym((res, env) -> new Ret(res, Prim.PRIMITIVE,
                env.cons(new Move_Id_Integer(res, num))));
    }
}

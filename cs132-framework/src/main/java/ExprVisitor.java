import cs132.IR.sparrow.*;
import cs132.IR.token.Identifier;
import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

public class ExprVisitor extends GJDepthFirst<T3<Identifier, Type, TransEnv>, T2<TypeEnv, TransEnv>> {

    @Override
    public T3<Identifier, Type, TransEnv> visit(Expression n, T2<TypeEnv, TransEnv> argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(AndExpression n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        return transEnv.genSym((res, env1) -> env1.genLabel((end, env2) -> {
            final var lExpr = n.f0.accept(this, new T2<>(typeEnv, env2));
            final var lSym = lExpr.a;
            final var lEnv = lExpr.c;

            final var rExpr = n.f2.accept(this, new T2<>(typeEnv, lEnv
                    .cons(new Move_Id_Id(res, lSym))
                    .cons(new IfGoto(lSym, end))));

            final var rSym = rExpr.a;
            final var rEnv = rExpr.c;

            return new T3<>(res, Prim.PRIMITIVE, rEnv
                    .cons(new Move_Id_Id(res, rSym))
                    .cons(new LabelInstr(end)));
        }));
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(CompareExpression n, T2<TypeEnv, TransEnv> argu) {
        return visitBinOp(n.f0, n.f2, argu, LessThan::new);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(PlusExpression n, T2<TypeEnv, TransEnv> argu) {
        return visitBinOp(n.f0, n.f2, argu, Add::new);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(MinusExpression n, T2<TypeEnv, TransEnv> argu) {
        return visitBinOp(n.f0, n.f2, argu, Subtract::new);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(TimesExpression n, T2<TypeEnv, TransEnv> argu) {
        return visitBinOp(n.f0, n.f2, argu, Multiply::new);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(ArrayLookup n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        return transEnv.genSym((res, env) -> {
            final var arrExpr = n.f0.accept(this, new T2<>(typeEnv, env));
            final var arrSym = arrExpr.a;
            final var arrEnv = arrExpr.c;

            final var arrChkEnv = arrEnv.nullCheck(arrSym);

            final var idxExpr = n.f2.accept(this, new T2<>(typeEnv, arrChkEnv));
            final var idxSym = idxExpr.a;
            final var idxEnv = idxExpr.c;

            final var idxChkEnv = idxEnv.idxCheck(arrSym, idxSym);

            return new T3<>(res, Prim.PRIMITIVE, idxChkEnv
                    .cons(new Move_Id_Integer(res, 4))
                    .cons(new Multiply(idxSym, idxSym, res))
                    .cons(new Add(arrSym, arrSym, idxSym))
                    .cons(new Load(res, arrSym, 4)));
        });

    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(ArrayLength n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        return transEnv.genSym((res, env) -> {
            final var arrExpr = n.f0.accept(this, new T2<>(typeEnv, env));
            final var arrSym = arrExpr.a;
            final var arrEnv = arrExpr.c;

            final var arrChkEnv = arrEnv.nullCheck(arrSym);

            return new T3<>(res, Prim.PRIMITIVE, arrChkEnv
                    .cons(new Load(res, arrSym, 0)));
        });
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(MessageSend n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var objExpr = n.f0.accept(this, argu);
        final var objSym = objExpr.a;
        final var objClass = (Class) objExpr.b;
        final var objEnv = objExpr.c;

        final var objChkEnv = objEnv.nullCheck(objSym);

        final var t1 = n.f4.accept(new FoldVisitor<>(new ExprVisitor(),
                (acc, exprRet) -> new T3<>(typeEnv, exprRet.c, acc.c.cons(exprRet.a))),
                new T3<>(typeEnv, objChkEnv, List.<Identifier>nul()));

        final var args = t1.c.reverse().cons(objSym).cons(TransEnv.statSym);
        final var argsEnv = t1.b;

        final var m = objClass.methodLookup(n.f2.f0.tokenImage).get();

        final var t2 = m.call(objSym, args, argsEnv);

        return new T3<>(t2.a, m.retType, t2.b);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(PrimaryExpression n, T2<TypeEnv, TransEnv> argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(IntegerLiteral n, T2<TypeEnv, TransEnv> argu) {
        return visitLiteral(Integer.parseInt(n.f0.tokenImage), argu);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(TrueLiteral n, T2<TypeEnv, TransEnv> argu) {
        return visitLiteral(1, argu);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(FalseLiteral n, T2<TypeEnv, TransEnv> argu) {
        return visitLiteral(0, argu);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(cs132.minijava.syntaxtree.Identifier n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        final var v = typeEnv.symLookup(n.f0.tokenImage).get();

        final var t = v.toTemp(transEnv);
        final var sym = t.a;
        final var env = t.b;

        return new T3<>(sym, v.type, env);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(ThisExpression n, T2<TypeEnv, TransEnv> argu) {
        return new T3<>(TransEnv.thisSym, argu.a.currClass.get(), argu.b);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(ArrayAllocationExpression n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        return transEnv.genSym((res, env1) -> env1.genSym((size, env2) -> env2.genLabel((ok, env3) -> {
            final var lenExpr = n.f3.accept(this, new T2<>(typeEnv, env3));
            final var lenSym = lenExpr.a;
            final var lenEnv = lenExpr.c;
            return new T3<>(res, Prim.PRIMITIVE, lenEnv
                    .cons(new Move_Id_Integer(res, 0))
                    .cons(new LessThan(res, lenSym, res))
                    .cons(new IfGoto(res, ok))
                    .cons(new ErrorMessage("\"array index out of bounds\""))
                    .cons(new LabelInstr(ok))
                    .cons(new Move_Id_Integer(res, 1))
                    .cons(new Add(size, lenSym, res))
                    .cons(new Move_Id_Integer(res, 4))
                    .cons(new Multiply(size, size, res))
                    .cons(new Alloc(res, size))
                    .cons(new Store(res, 0, lenSym)));
        })));
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(AllocationExpression n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        final var className = n.f1.f0.tokenImage;
        final var cls = typeEnv.classLookup(className);
        final var t = cls.alloc(transEnv);
        return new T3<>(t.a, cls, t.b);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(NotExpression n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        return transEnv.genSym((res, env) -> {
            final var opdExpr = n.f1.accept(this, new T2<>(typeEnv, env));
            final var opdSym = opdExpr.a;
            final var opdEnv = opdExpr.c;
            return new T3<>(res, Prim.PRIMITIVE, opdEnv
                    .cons(new Move_Id_Integer(res, 1))
                    .cons(new Subtract(res, res, opdSym)));
        });
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(BracketExpression n, T2<TypeEnv, TransEnv> argu) {
        return n.f1.accept(this, argu);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(ExpressionRest n, T2<TypeEnv, TransEnv> argu) {
        return n.f1.accept(this, argu);
    }

    T3<Identifier, Type, TransEnv> visitBinOp(Node lhs, Node rhs, T2<TypeEnv, TransEnv> argu,
            F3<Identifier, Identifier, Identifier, Instruction> mkInstr) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        return transEnv.genSym((res, env) -> {
            final var lExpr = lhs.accept(this, argu);
            final var lSym = lExpr.a;
            final var lEnv = lExpr.c;
            final var rExpr = rhs.accept(this, new T2<>(typeEnv, lEnv));
            final var rSym = rExpr.a;
            final var rEnv = rExpr.c;
            return new T3<>(res, Prim.PRIMITIVE, rEnv.cons(mkInstr.apply(res, lSym, rSym)));
        });
    }

    T3<Identifier, Type, TransEnv> visitLiteral(int num, T2<TypeEnv, TransEnv> argu) {
        return argu.b.genSym((res, env) -> new T3<>(res, Prim.PRIMITIVE,
                env.cons(new Move_Id_Integer(res, num))));
    }
}

import java.util.function.*;

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

        final var t1 = transEnv.genSym();
        final var eSym = t1.a;
        final var t2 = t1.b.genLabel();
        final var endLabel = t2.a;
        final var env = t2.b;

        final var lExpr = n.f0.accept(this, new T2<>(typeEnv, env));
        final var lSym = lExpr.a;
        final var lEnv = lExpr.c;

        // Short circuit
        final var scEnv = lEnv.join(List.<Instruction>nul()
                .cons(new IfGoto(lSym, endLabel))
                .cons(new Move_Id_Id(eSym, lSym)));

        final var rExpr = n.f2.accept(this, new T2<>(typeEnv, scEnv));
        final var rSym = rExpr.a;
        final var rEnv = rExpr.c;

        final var zEnv = rEnv.join(List.<Instruction>nul()
                .cons(new LabelInstr(endLabel))
                .cons(new Move_Id_Id(eSym, rSym)));

        return new T3<>(eSym, Prim.BOOL, zEnv);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(CompareExpression n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;

        final var t = transEnv.genSym();
        final var eSym = t.a;
        final var env = t.b;
        final var zEnv = visitBinOp(n.f0, n.f2, new T2<>(typeEnv, env),
                (lSym, rSym) -> List.of(new LessThan(eSym, lSym, rSym)));

        return new T3<>(eSym, Prim.BOOL, zEnv);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(PlusExpression n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        final var t = transEnv.genSym();
        final var eSym = t.a;
        final var env = t.b;
        final var zEnv = visitBinOp(n.f0, n.f2, new T2<>(typeEnv, env),
                (lSym, rSym) -> List.of(new Add(eSym, lSym, rSym)));
        return new T3<>(eSym, Prim.INT, zEnv);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(MinusExpression n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        final var t = transEnv.genSym();
        final var eSym = t.a;
        final var env = t.b;
        final var zEnv = visitBinOp(n.f0, n.f2, new T2<>(typeEnv, env),
                (lSym, rSym) -> List.of(new Subtract(eSym, lSym, rSym)));
        return new T3<>(eSym, Prim.INT, zEnv);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(TimesExpression n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        final var t = transEnv.genSym();
        final var eSym = t.a;
        final var env = t.b;
        final var zEnv = visitBinOp(n.f0, n.f2, new T2<>(typeEnv, env),
                (lSym, rSym) -> List.of(new Multiply(eSym, lSym, rSym)));
        return new T3<>(eSym, Prim.INT, zEnv);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(ArrayLookup n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        final var t1 = transEnv.genSym();
        final var eSym = t1.a;
        final var t2 = t1.b.genLabel();
        final var errLabel = t2.a;
        final var t3 = t2.b.genLabel();
        final var endLabel = t3.a;
        final var env = t3.b;

        final var zEnv = visitBinOp(n.f0, n.f2, new T2<>(typeEnv, env),
                (arrSym, idxSym) -> List.<Instruction>nul()
                        .cons(new LabelInstr(endLabel))
                        .cons(new ErrorMessage("\"array index out of bounds\""))
                        .cons(new LabelInstr(errLabel))
                        .cons(new Goto(endLabel))
                        .cons(new Load(eSym, arrSym, 4))
                        .cons(new Add(arrSym, arrSym, idxSym))
                        .cons(new Multiply(idxSym, idxSym, eSym))
                        .cons(new Move_Id_Integer(eSym, 4))
                        .cons(new IfGoto(eSym, errLabel))
                        .cons(new LessThan(eSym, idxSym, eSym))
                        .cons(new Load(eSym, arrSym, 0)));

        return new T3<>(eSym, Prim.INT, zEnv);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(ArrayLength n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        final var t = transEnv.genSym();
        final var eSym = t.a;
        final var env = t.b;
        final var arrExpr = n.f0.accept(this, new T2<>(typeEnv, env));
        final var arrSym = arrExpr.a;
        final var arrEnv = arrExpr.c;
        final var zEnv = arrEnv.join(List.<Instruction>of(new Load(eSym, arrSym, 0)));
        return new T3<>(eSym, Prim.INT, zEnv);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(MessageSend n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var objExpr = n.f0.accept(this, argu);
        final var objSym = objExpr.a;
        final var objClass = (Class) objExpr.b;
        final var objEnv = objExpr.c;

        final var t = n.f4.accept(new FoldVisitor<>(new ExprVisitor(),
                (u, ret) -> new T3<>(typeEnv, ret.c, u.c.cons(ret.a))),
                new T3<>(typeEnv, objEnv, List.<Identifier>nul()));
        final var args = t.c.reverse().cons(objSym).cons(TransEnv.statSym);
        final var argsEnv = t.b;

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
        final var num = Integer.parseInt(n.f0.tokenImage);
        final var t = argu.b.genSym();
        final var eSym = t.a;
        final var env = t.b;
        return new T3<>(eSym, Prim.BOOL, env.join(List.of(new Move_Id_Integer(eSym, num))));
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(TrueLiteral n, T2<TypeEnv, TransEnv> argu) {
        final var t = argu.b.genSym();
        final var eSym = t.a;
        final var env = t.b;
        return new T3<>(eSym, Prim.BOOL, env.join(List.of(new Move_Id_Integer(eSym, 1))));
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(FalseLiteral n, T2<TypeEnv, TransEnv> argu) {
        final var t = argu.b.genSym();
        final var eSym = t.a;
        final var env = t.b;
        return new T3<>(eSym, Prim.BOOL, env.join(List.of(new Move_Id_Integer(eSym, 0))));
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
        return new T3<>(TransEnv.thisSym(), argu.a.currClass.get(), argu.b);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(ArrayAllocationExpression n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        final var t1 = transEnv.genSym();
        final var eSym = t1.a;
        final var t2 = t1.b.genSym();
        final var sizeSym = t2.a;
        final var t3 = t2.b.genLabel();
        final var goodLabel = t3.a;
        final var env = t3.b;
        final var lenExpr = n.f3.accept(this, new T2<>(typeEnv, env));
        final var lenSym = lenExpr.a;
        final var lenEnv = lenExpr.c;
        final var zEnv = lenEnv.join(List.<Instruction>nul()
                .cons(new Store(eSym, 0, lenSym))
                .cons(new Alloc(eSym, sizeSym))
                .cons(new Multiply(sizeSym, sizeSym, eSym))
                .cons(new Move_Id_Integer(eSym, 4))
                .cons(new Add(sizeSym, lenSym, eSym))
                .cons(new Move_Id_Integer(eSym, 1))
                .cons(new LabelInstr(goodLabel))
                .cons(new ErrorMessage("\"invalid array length\""))
                .cons(new IfGoto(eSym, goodLabel))
                .cons(new LessThan(eSym, lenSym, eSym))
                .cons(new Move_Id_Integer(eSym, 0)));
        return new T3<>(eSym, Prim.ARR, zEnv);
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
        final var t = transEnv.genSym();
        final var eSym = t.a;
        final var env = t.b;
        final var opdExpr = n.f1.accept(this, new T2<>(typeEnv, env));
        final var opdSym = opdExpr.a;
        final var opdEnv = opdExpr.c;
        final var zEnv = opdEnv.join(List.<Instruction>nul()
                .cons(new Subtract(eSym, eSym, opdSym))
                .cons(new Move_Id_Integer(eSym, 1)));
        return new T3<>(eSym, Prim.BOOL, zEnv);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(BracketExpression n, T2<TypeEnv, TransEnv> argu) {
        return n.f1.accept(this, argu);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(ExpressionRest n, T2<TypeEnv, TransEnv> argu) {
        return n.f1.accept(this, argu);
    }

    TransEnv visitBinOp(Node lhs, Node rhs, T2<TypeEnv, TransEnv> argu,
            BiFunction<Identifier, Identifier, List<Instruction>> cont) {
        final var typeEnv = argu.a;
        final var lExpr = lhs.accept(this, argu);
        final var lSym = lExpr.a;
        final var lEnv = lExpr.c;
        final var rExpr = rhs.accept(this, new T2<>(typeEnv, lEnv));
        final var rSym = rExpr.a;
        final var rEnv = rExpr.c;
        return rEnv.join(cont.apply(lSym, rSym));
    }
}

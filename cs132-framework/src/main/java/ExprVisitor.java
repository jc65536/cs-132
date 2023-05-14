import java.util.function.*;

import cs132.IR.sparrow.*;
import cs132.IR.token.Identifier;
import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

public class ExprVisitor extends GJDepthFirst<T3<Identifier, Type, TransEnv>, TransEnv> {

    @Override
    public T3<Identifier, Type, TransEnv> visit(Expression n, TransEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(AndExpression n, TransEnv argu) {
        final var t1 = argu.genSym();
        final var eSym = t1.a;
        final var t2 = t1.b.genLabel();
        final var endLabel = t2.a;
        final var env = t2.b;

        final var lExpr = n.f0.accept(this, env);
        final var lSym = lExpr.a;
        final var lEnv = lExpr.c;

        // Short circuit
        final var scEnv = lEnv.join(List.<Instruction>nul()
                .cons(new IfGoto(lSym, endLabel))
                .cons(new Move_Id_Id(eSym, lSym)));

        final var rExpr = n.f2.accept(this, scEnv);
        final var rSym = rExpr.a;
        final var rEnv = rExpr.c;

        final var zEnv = rEnv.join(List.<Instruction>nul()
                .cons(new LabelInstr(endLabel))
                .cons(new Move_Id_Id(eSym, rSym)));

        return new T3<>(eSym, Prim.BOOL, zEnv);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(CompareExpression n, TransEnv argu) {
        final var t = argu.genSym();
        final var eSym = t.a;
        final var env = t.b;
        final var zEnv = visitBinOp(n.f0, n.f2, env,
                (lSym, rSym) -> List.of(new LessThan(eSym, lSym, rSym)));
        return new T3<>(eSym, Prim.BOOL, zEnv);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(PlusExpression n, TransEnv argu) {
        final var t = argu.genSym();
        final var eSym = t.a;
        final var env = t.b;
        final var zEnv = visitBinOp(n.f0, n.f2, env,
                (lSym, rSym) -> List.of(new LessThan(eSym, lSym, rSym)));
        return new T3<>(eSym, Prim.INT, zEnv);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(MinusExpression n, TransEnv argu) {
        final var t = argu.genSym();
        final var eSym = t.a;
        final var env = t.b;
        final var zEnv = visitBinOp(n.f0, n.f2, env,
                (lSym, rSym) -> List.of(new LessThan(eSym, lSym, rSym)));
        return new T3<>(eSym, Prim.INT, zEnv);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(TimesExpression n, TransEnv argu) {
        final var t = argu.genSym();
        final var eSym = t.a;
        final var env = t.b;
        final var zEnv = visitBinOp(n.f0, n.f2, env,
                (lSym, rSym) -> List.of(new LessThan(eSym, lSym, rSym)));
        return new T3<>(eSym, Prim.INT, zEnv);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(ArrayLookup n, TransEnv argu) {
        final var t1 = argu.genSym();
        final var eSym = t1.a;
        final var t2 = t1.b.genLabel();
        final var errLabel = t2.a;
        final var t3 = t2.b.genLabel();
        final var endLabel = t3.a;
        final var env = t3.b;

        final var zEnv = visitBinOp(n.f0, n.f2, env,
                (arrSym, idxSym) -> List.<Instruction>nul()
                        .cons(new LabelInstr(endLabel))
                        .cons(new ErrorMessage("Array index out of bounds"))
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
    public T3<Identifier, Type, TransEnv> visit(ArrayLength n, TransEnv argu) {
        final var t = argu.genSym();
        final var eSym = t.a;
        final var env = t.b;
        final var arrExpr = n.f0.accept(this, env);
        final var arrSym = arrExpr.a;
        final var arrEnv = arrExpr.c;
        final var zEnv = arrEnv.join(List.<Instruction>of(new Load(eSym, arrSym, 0)));
        return new T3<>(eSym, Prim.INT, zEnv);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(MessageSend n, TransEnv argu) {
        // TODO
        return null;
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(PrimaryExpression n, TransEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(IntegerLiteral n, TransEnv argu) {
        final var num = Integer.parseInt(n.f0.tokenImage);
        final var t = argu.genSym();
        final var eSym = t.a;
        final var env = t.b;
        return new T3<>(eSym, Prim.BOOL, env.join(List.of(new Move_Id_Integer(eSym, num))));
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(TrueLiteral n, TransEnv argu) {
        final var t = argu.genSym();
        final var eSym = t.a;
        final var env = t.b;
        return new T3<>(eSym, Prim.BOOL, env.join(List.of(new Move_Id_Integer(eSym, 1))));
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(FalseLiteral n, TransEnv argu) {
        final var t = argu.genSym();
        final var eSym = t.a;
        final var env = t.b;
        return new T3<>(eSym, Prim.BOOL, env.join(List.of(new Move_Id_Integer(eSym, 0))));
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(cs132.minijava.syntaxtree.Identifier n, TransEnv argu) {
        final var v = argu.symLookup(n.f0.tokenImage).get();

        final var t = v.toTemp(argu);
        final var sym = t.a;
        final var env = t.b;

        return new T3<>(sym, v.type, env);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(ThisExpression n, TransEnv argu) {
        return new T3<>(TransEnv.thisSym(), argu.currClass.get(), argu);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(ArrayAllocationExpression n, TransEnv argu) {
        final var t1 = argu.genSym();
        final var eSym = t1.a;
        final var t2 = t1.b.genLabel();
        final var goodLabel = t2.a;
        final var env = t2.b;
        final var lenExpr = n.f3.accept(this, env);
        final var lenSym = lenExpr.a;
        final var lenEnv = lenExpr.c;
        final var zEnv = lenEnv.join(List.<Instruction>nul()
                .cons(new Alloc(eSym, lenSym))
                .cons(new Multiply(lenSym, lenSym, eSym))
                .cons(new Move_Id_Integer(eSym, 4))
                .cons(new Add(lenSym, lenSym, eSym))
                .cons(new Move_Id_Integer(eSym, 1))
                .cons(new LabelInstr(goodLabel))
                .cons(new ErrorMessage("Invalid array length"))
                .cons(new IfGoto(eSym, goodLabel))
                .cons(new LessThan(eSym, lenSym, eSym))
                .cons(new Move_Id_Integer(eSym, 0)));
        return new T3<>(eSym, Prim.ARR, zEnv);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(AllocationExpression n, TransEnv argu) {
        // TODO
        return super.visit(n, argu);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(NotExpression n, TransEnv argu) {
        final var t = argu.genSym();
        final var eSym = t.a;
        final var env = t.b;
        final var opdExpr = n.f1.accept(this, env);
        final var opdSym = opdExpr.a;
        final var opdEnv = opdExpr.c;
        final var zEnv = opdEnv.join(List.<Instruction>nul()
                .cons(new Subtract(eSym, eSym, opdSym))
                .cons(new Move_Id_Integer(eSym, 1)));
        return new T3<>(eSym, Prim.BOOL, zEnv);
    }

    @Override
    public T3<Identifier, Type, TransEnv> visit(BracketExpression n, TransEnv argu) {
        return n.f1.accept(this, argu);
    }

    TransEnv visitBinOp(Node lhs, Node rhs, TransEnv argu,
            BiFunction<Identifier, Identifier, List<Instruction>> cont) {
        final var lExpr = lhs.accept(this, argu);
        final var lSym = lExpr.a;
        final var lEnv = lExpr.c;
        final var rExpr = rhs.accept(this, lEnv);
        final var rSym = rExpr.a;
        final var rEnv = rExpr.c;
        return rEnv.join(cont.apply(lSym, rSym));
    }
}

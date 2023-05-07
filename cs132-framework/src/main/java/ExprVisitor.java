import java.util.function.*;

import cs132.IR.sparrow.Add;
import cs132.IR.sparrow.ErrorMessage;
import cs132.IR.sparrow.Goto;
import cs132.IR.sparrow.IfGoto;
import cs132.IR.sparrow.Instruction;
import cs132.IR.sparrow.LabelInstr;
import cs132.IR.sparrow.LessThan;
import cs132.IR.sparrow.Load;
import cs132.IR.sparrow.Move_Id_Id;
import cs132.IR.sparrow.Move_Id_Integer;
import cs132.IR.sparrow.Multiply;
import cs132.IR.sparrow.Subtract;
import cs132.IR.token.Identifier;
import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

public class ExprVisitor extends GJDepthFirst<TransEnv, TransEnv> {

    @Override
    public TransEnv visit(Expression n, TransEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public TransEnv visit(AndExpression n, TransEnv argu) {
        final var e1 = argu.genSym();
        final var eSym = e1.t;
        final var e2 = e1.genLabel();
        final var endLabel = e2.t;
        final var env = e2;

        final var lSym = env.nextSym();

        final var rEnv = n.f0.accept(this, env)
                .join(List.<Instruction>of(new IfGoto(lSym, endLabel))
                        .cons(new Move_Id_Id(eSym, lSym)));

        final var rSym = rEnv.nextSym();

        return n.f2.accept(this, rEnv)
                .join(List.<Instruction>of(new LabelInstr(endLabel))
                        .cons(new Move_Id_Id(eSym, rSym)));
    }

    @Override
    public TransEnv visit(CompareExpression n, TransEnv argu) {
        final var env = argu.genSym();
        final var eSym = env.t;
        return visitBinOp(n.f0, n.f2, env,
                (lSym, rSym) -> List.of(new LessThan(eSym, lSym, rSym)));
    }

    @Override
    public TransEnv visit(PlusExpression n, TransEnv argu) {
        final var env = argu.genSym();
        final var eSym = env.t;
        return visitBinOp(n.f0, n.f2, env,
                (lSym, rSym) -> List.of(new Add(eSym, lSym, rSym)));
    }

    @Override
    public TransEnv visit(MinusExpression n, TransEnv argu) {
        final var env = argu.genSym();
        final var eSym = env.t;
        return visitBinOp(n.f0, n.f2, env,
                (lSym, rSym) -> List.of(new Subtract(eSym, lSym, rSym)));
    }

    @Override
    public TransEnv visit(TimesExpression n, TransEnv argu) {
        final var env = argu.genSym();
        final var eSym = env.t;
        return visitBinOp(n.f0, n.f2, env,
                (lSym, rSym) -> List.of(new Multiply(eSym, lSym, rSym)));
    }

    @Override
    public TransEnv visit(ArrayLookup n, TransEnv argu) {
        final var e1 = argu.genSym();
        final var eSym = e1.t;
        final var e2 = e1.genLabel();
        final var errLabel = e2.t;
        final var e3 = e2.genLabel();
        final var endLabel = e3.t;
        final var env = e3;

        return visitBinOp(n.f0, n.f2, env,
                (arrSym, idxSym) -> List.<Instruction>of(new LabelInstr(endLabel))
                        .cons(new ErrorMessage("Array index out of bounds"))
                        .cons(new LabelInstr(errLabel))
                        .cons(new Goto(endLabel))
                        .cons(new Load(eSym, arrSym, 1))
                        .cons(new Add(arrSym, arrSym, idxSym))
                        .cons(new IfGoto(eSym, errLabel))
                        .cons(new LessThan(eSym, idxSym, eSym))
                        .cons(new Load(eSym, arrSym, 0)));
    }

    @Override
    public TransEnv visit(ArrayLength n, TransEnv argu) {
        final var env = argu.genSym();
        final var eSym = env.t;
        final var arrSym = env.nextSym();
        return n.f0.accept(this, env)
                .join(List.<Instruction>of(new Load(eSym, arrSym, 0)));
    }

    @Override
    public TransEnv visit(MessageSend n, TransEnv argu) {
        // TODO
        return null;
    }

    @Override
    public TransEnv visit(PrimaryExpression n, TransEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public TransEnv visit(IntegerLiteral n, TransEnv argu) {
        return assignInt(Integer.parseInt(n.f0.tokenImage), argu);
    }

    @Override
    public TransEnv visit(TrueLiteral n, TransEnv argu) {
        return assignInt(1, argu);
    }

    @Override
    public TransEnv visit(FalseLiteral n, TransEnv argu) {
        return assignInt(0, argu);
    }

    @Override
    public TransEnv visit(cs132.minijava.syntaxtree.Identifier n, TransEnv argu) {
        // TODO
        return null;
    }

    @Override
    public TransEnv visit(ThisExpression n, TransEnv argu) {
        // TODO
        return null;
    }

    TransEnv visitBinOp(Node lhs, Node rhs, TransEnv argu,
            BiFunction<Identifier, Identifier, List<Instruction>> cont) {
        final var lSym = argu.nextSym();
        final var rEnv = lhs.accept(this, argu);
        final var rSym = rEnv.nextSym();
        return rhs.accept(this, rEnv).join(cont.apply(lSym, rSym));
    }

    TransEnv assignInt(int n, TransEnv argu) {
        final var env = argu.genSym();
        final var eSym = env.t;
        return env.join(List.of(new Move_Id_Integer(eSym, n)));
    }
}

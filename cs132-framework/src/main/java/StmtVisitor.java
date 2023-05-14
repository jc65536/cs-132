import cs132.IR.sparrow.*;
import cs132.minijava.syntaxtree.*;
import cs132.minijava.syntaxtree.Block;
import cs132.minijava.visitor.*;

public class StmtVisitor extends GJDepthFirst<TransEnv, TransEnv> {
    @Override
    public TransEnv visit(Statement n, TransEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public TransEnv visit(Block n, TransEnv argu) {
        return n.f1.nodes.stream().reduce(argu,
                (acc, node) -> node.accept(this, acc),
                (u, v) -> v);
    }

    @Override
    public TransEnv visit(AssignmentStatement n, TransEnv argu) {
        final var name = n.f0.f0.tokenImage;
        final var rExpr = n.f2.accept(new ExprVisitor(), argu);
        final var rSym = rExpr.a;
        final var rEnv = rExpr.c;
        return argu.symLookup(name).get().assign(rSym, rEnv);
    }

    @Override
    public TransEnv visit(ArrayAssignmentStatement n, TransEnv argu) {
        final var t1 = argu.genLabel();
        final var errLabel = t1.a;
        final var t2 = t1.b.genLabel();
        final var endLabel = t2.a;
        final var t3 = t2.b.genSym();
        final var tSym = t3.a;
        final var env = t3.b;

        final var arrExpr = n.f0.accept(new ExprVisitor(), env);
        final var arrSym = arrExpr.a;
        final var arrEnv = arrExpr.c;
        final var idxExpr = n.f2.accept(new ExprVisitor(), arrEnv);
        final var idxSym = idxExpr.a;
        final var idxEnv = idxExpr.c;

        // Index check
        final var idxChkEnv = idxEnv.join(List.<Instruction>nul()
                .cons(new IfGoto(tSym, errLabel))
                .cons(new LessThan(tSym, idxSym, tSym))
                .cons(new Load(tSym, arrSym, 0)));

        final var rExpr = n.f5.accept(new ExprVisitor(), idxChkEnv);
        final var rSym = rExpr.a;
        final var rEnv = rExpr.c;

        return rEnv.join(List.<Instruction>nul()
                .cons(new LabelInstr(endLabel))
                .cons(new ErrorMessage("Array index out of bounds"))
                .cons(new LabelInstr(errLabel))
                .cons(new Goto(endLabel))
                .cons(new Store(arrSym, 4, rSym))
                .cons(new Add(arrSym, arrSym, idxSym))
                .cons(new Multiply(idxSym, idxSym, tSym))
                .cons(new Move_Id_Integer(tSym, 4)));
    }

    @Override
    public TransEnv visit(IfStatement n, TransEnv argu) {
        final var t1 = argu.genLabel();
        final var elseLabel = t1.a;
        final var t2 = t1.b.genLabel();
        final var endLabel = t2.a;
        final var env = t2.b;

        final var condExpr = n.f2.accept(new ExprVisitor(), env);
        final var condSym = condExpr.a;
        final var condEnv = condExpr.c;

        final var jmpEnv = condEnv.join(List.of(new IfGoto(condSym, elseLabel)));

        final var ifEnv = n.f4.accept(this, jmpEnv).join(List.<Instruction>nul()
                .cons(new LabelInstr(elseLabel))
                .cons(new Goto(endLabel)));

        return n.f6.accept(this, ifEnv).join(List.of(new LabelInstr(endLabel)));
    }

    @Override
    public TransEnv visit(WhileStatement n, TransEnv argu) {
        final var t1 = argu.genLabel();
        final var loopLabel = t1.a;
        final var t2 = t1.b.genLabel();
        final var endLabel = t2.a;
        final var env = t2.b;

        final var condExpr = n.f2.accept(new ExprVisitor(), env.join(List.of(new LabelInstr(loopLabel))));
        final var condSym = condExpr.a;
        final var condEnv = condExpr.c;

        final var jmpEnv = condEnv.join(List.of(new IfGoto(condSym, endLabel)));

        return n.f4.accept(this, jmpEnv).join(List.of(new Goto(loopLabel)));
    }

    @Override
    public TransEnv visit(PrintStatement n, TransEnv argu) {
        final var argExpr = n.f2.accept(new ExprVisitor(), argu);
        final var argSym = argExpr.a;
        final var argEnv = argExpr.c;

        return argEnv.join(List.of(new Print(argSym)));
    }
}

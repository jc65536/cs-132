import cs132.IR.sparrow.*;
import cs132.minijava.syntaxtree.*;
import cs132.minijava.syntaxtree.Block;
import cs132.minijava.visitor.*;

public class StmtVisitor extends GJDepthFirst<TransEnv, T2<TypeEnv, TransEnv>> {
    @Override
    public TransEnv visit(Statement n, T2<TypeEnv, TransEnv> argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public TransEnv visit(Block n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        return n.f1.nodes.stream().reduce(argu,
                (acc, node) -> new T2<>(typeEnv, node.accept(this, acc)),
                (u, v) -> v).b;
    }

    @Override
    public TransEnv visit(AssignmentStatement n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var name = n.f0.f0.tokenImage;
        final var rExpr = n.f2.accept(new ExprVisitor(), argu);
        final var rSym = rExpr.a;
        final var rEnv = rExpr.c;
        return typeEnv.symLookup(name).get().assign(rSym, rEnv);
    }

    @Override
    public TransEnv visit(ArrayAssignmentStatement n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        final var t1 = transEnv.genLabel();
        final var errLabel = t1.a;
        final var t2 = t1.b.genLabel();
        final var endLabel = t2.a;
        final var t3 = t2.b.genSym();
        final var tSym = t3.a;
        final var env = t3.b;

        final var arrExpr = n.f0.accept(new ExprVisitor(), new T2<>(typeEnv, env));
        final var arrSym = arrExpr.a;
        final var arrEnv = arrExpr.c;
        final var idxExpr = n.f2.accept(new ExprVisitor(), new T2<>(typeEnv, arrEnv));
        final var idxSym = idxExpr.a;
        final var idxEnv = idxExpr.c;

        // Index check
        final var idxChkEnv = idxEnv.join(List.<Instruction>nul()
                .cons(new IfGoto(tSym, errLabel))
                .cons(new LessThan(tSym, idxSym, tSym))
                .cons(new Load(tSym, arrSym, 0)));

        final var rExpr = n.f5.accept(new ExprVisitor(), new T2<>(typeEnv, idxChkEnv));
        final var rSym = rExpr.a;
        final var rEnv = rExpr.c;

        return rEnv.join(List.<Instruction>nul()
                .cons(new LabelInstr(endLabel))
                .cons(new ErrorMessage("\"array index out of bounds\""))
                .cons(new LabelInstr(errLabel))
                .cons(new Goto(endLabel))
                .cons(new Store(arrSym, 4, rSym))
                .cons(new Add(arrSym, arrSym, idxSym))
                .cons(new Multiply(idxSym, idxSym, tSym))
                .cons(new Move_Id_Integer(tSym, 4)));
    }

    @Override
    public TransEnv visit(IfStatement n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        final var t1 = transEnv.genLabel();
        final var elseLabel = t1.a;
        final var t2 = t1.b.genLabel();
        final var endLabel = t2.a;
        final var env = t2.b;

        final var condExpr = n.f2.accept(new ExprVisitor(), new T2<>(typeEnv, env));
        final var condSym = condExpr.a;
        final var condEnv = condExpr.c;

        final var jmpEnv = condEnv.join(List.of(new IfGoto(condSym, elseLabel)));

        final var ifEnv = n.f4.accept(this, new T2<>(typeEnv, jmpEnv))
                .join(List.<Instruction>nul()
                        .cons(new LabelInstr(elseLabel))
                        .cons(new Goto(endLabel)));

        return n.f6.accept(this, new T2<>(typeEnv, ifEnv))
                .join(List.of(new LabelInstr(endLabel)));
    }

    @Override
    public TransEnv visit(WhileStatement n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        final var t1 = transEnv.genLabel();
        final var loopLabel = t1.a;
        final var t2 = t1.b.genLabel();
        final var endLabel = t2.a;
        final var env = t2.b;

        final var condExpr = n.f2.accept(new ExprVisitor(),
                new T2<>(typeEnv, env.join(List.of(new LabelInstr(loopLabel)))));
        final var condSym = condExpr.a;
        final var condEnv = condExpr.c;

        final var jmpEnv = condEnv.join(List.of(new IfGoto(condSym, endLabel)));

        return n.f4.accept(this, new T2<>(typeEnv, jmpEnv))
                .join(List.<Instruction>of(new LabelInstr(endLabel))
                        .cons(new Goto(loopLabel)));
    }

    @Override
    public TransEnv visit(PrintStatement n, T2<TypeEnv, TransEnv> argu) {
        final var argExpr = n.f2.accept(new ExprVisitor(), argu);
        final var argSym = argExpr.a;
        final var argEnv = argExpr.c;
        return argEnv.join(List.of(new Print(argSym)));
    }
}

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
        return transEnv.genSym((tmp, env) -> {
            final var arrExpr = n.f0.accept(new ExprVisitor(), new T2<>(typeEnv, env));
            final var arrSym = arrExpr.a;
            final var arrEnv = arrExpr.c;

            final var arrChkEnv = arrEnv.nullCheck(arrSym);

            final var idxExpr = n.f2.accept(new ExprVisitor(), new T2<>(typeEnv, arrChkEnv));
            final var idxSym = idxExpr.a;
            final var idxEnv = idxExpr.c;

            // Index check
            final var idxChkEnv = idxEnv.idxCheck(arrSym, idxSym);

            final var rExpr = n.f5.accept(new ExprVisitor(), new T2<>(typeEnv, idxChkEnv));
            final var rSym = rExpr.a;
            final var rEnv = rExpr.c;

            return rEnv.join(List.<Instruction>nul()
                    .cons(new Store(arrSym, 4, rSym))
                    .cons(new Add(arrSym, arrSym, idxSym))
                    .cons(new Multiply(idxSym, idxSym, tmp))
                    .cons(new Move_Id_Integer(tmp, 4)));
        });
    }

    @Override
    public TransEnv visit(IfStatement n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        return transEnv.genLabel((fail, env1) -> env1.genLabel((end, env2) -> {
            final var condExpr = n.f2.accept(new ExprVisitor(), new T2<>(typeEnv, env2));
            final var condSym = condExpr.a;
            final var condEnv = condExpr.c;

            final var jmpEnv = condEnv.join(List.of(new IfGoto(condSym, fail)));

            final var ifEnv = n.f4.accept(this, new T2<>(typeEnv, jmpEnv))
                    .join(List.<Instruction>nul()
                            .cons(new LabelInstr(fail))
                            .cons(new Goto(end)));

            return n.f6.accept(this, new T2<>(typeEnv, ifEnv))
                    .join(List.of(new LabelInstr(end)));
        }));
    }

    @Override
    public TransEnv visit(WhileStatement n, T2<TypeEnv, TransEnv> argu) {
        final var typeEnv = argu.a;
        final var transEnv = argu.b;
        return transEnv.genLabel((start, env1) -> env1.genLabel((end, env2) -> {
            final var condExpr = n.f2.accept(new ExprVisitor(),
                    new T2<>(typeEnv, env2.join(List.of(new LabelInstr(start)))));
            final var condSym = condExpr.a;
            final var condEnv = condExpr.c;

            final var jmpEnv = condEnv.join(List.of(new IfGoto(condSym, end)));

            return n.f4.accept(this, new T2<>(typeEnv, jmpEnv))
                    .join(List.<Instruction>of(new LabelInstr(end))
                            .cons(new Goto(start)));
        }));
    }

    @Override
    public TransEnv visit(PrintStatement n, T2<TypeEnv, TransEnv> argu) {
        final var argExpr = n.f2.accept(new ExprVisitor(), argu);
        final var argSym = argExpr.a;
        final var argEnv = argExpr.c;
        return argEnv.join(List.of(new Print(argSym)));
    }
}

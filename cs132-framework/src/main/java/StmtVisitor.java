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
        return n.f1.nodes.stream().reduce(argu,
                (acc, node) -> acc.setB(node.accept(this, acc)),
                (u, v) -> v).b;
    }

    @Override
    public TransEnv visit(AssignmentStatement n, T2<TypeEnv, TransEnv> argu) {
        final var name = n.f0.f0.tokenImage;
        final var rhs = n.f2.accept(new ExprVisitor(), argu);
        return argu.a.symLookup(name).assign(rhs.sym, rhs.env);
    }

    @Override
    public TransEnv visit(ArrayAssignmentStatement n, T2<TypeEnv, TransEnv> argu) {
        return argu.b.genSym((tmp, env) -> {
            final var arr = n.f0.accept(new ExprVisitor(), argu.setB(env));
            final var idx = n.f2.accept(new ExprVisitor(), argu.setB(arr.env.nullCheck(arr.sym)));
            final var rhs = n.f5.accept(new ExprVisitor(), argu.setB(idx.env.idxCheck(arr.sym, idx.sym)));
            return rhs.env.cons(new Move_Id_Integer(tmp, 4))
                    .cons(new Multiply(idx.sym, idx.sym, tmp))
                    .cons(new Add(arr.sym, arr.sym, idx.sym))
                    .cons(new Store(arr.sym, 4, rhs.sym));
        });
    }

    @Override
    public TransEnv visit(IfStatement n, T2<TypeEnv, TransEnv> argu) {
        return argu.b.genLabel((fail, env1) -> env1.genLabel((end, env2) -> {
            final var cond = n.f2.accept(new ExprVisitor(), argu.setB(env2));
            final var ifStmt = n.f4.accept(this, argu.setB(cond.env
                    .cons(new IfGoto(cond.sym, fail))))
                    .cons(new Goto(end))
                    .cons(new LabelInstr(fail));
            return n.f6.accept(this, argu.setB(ifStmt))
                    .cons(new LabelInstr(end));
        }));
    }

    @Override
    public TransEnv visit(WhileStatement n, T2<TypeEnv, TransEnv> argu) {
        return argu.b.genLabel((start, env1) -> env1.genLabel((end, env2) -> {
            final var cond = n.f2.accept(new ExprVisitor(),
                    argu.setB(env2.cons(new LabelInstr(start))));
            return n.f4.accept(this, argu.setB(cond.env
                    .cons(new IfGoto(cond.sym, end))))
                    .cons(new Goto(start))
                    .cons(new LabelInstr(end));
        }));
    }

    @Override
    public TransEnv visit(PrintStatement n, T2<TypeEnv, TransEnv> argu) {
        final var arg = n.f2.accept(new ExprVisitor(), argu);
        return arg.env.cons(new Print(arg.sym));
    }
}

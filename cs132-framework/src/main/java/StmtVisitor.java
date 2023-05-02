import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

public class StmtVisitor extends GJDepthFirst<Boolean, TypeEnv> {
    @Override
    public Boolean visit(Statement n, TypeEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Boolean visit(Block n, TypeEnv argu) {
        return n.f1.nodes.stream().allMatch(node -> node.accept(this, argu));
    }

    @Override
    public Boolean visit(AssignmentStatement n, TypeEnv argu) {
        final var destName = n.f0.f0.tokenImage;
        final var destType = argu.symLookup(destName).type;
        return Util.checkExpr(n.f2, destType, argu);
    }

    @Override
    public Boolean visit(ArrayAssignmentStatement n, TypeEnv argu) {
        return Util.checkExpr(n.f0, Prim.ARR, argu)
                && Util.checkExpr(n.f2, Prim.INT, argu)
                && Util.checkExpr(n.f5, Prim.INT, argu);
    }

    @Override
    public Boolean visit(IfStatement n, TypeEnv argu) {
        return Util.checkExpr(n.f2, Prim.BOOL, argu)
                && n.f4.accept(this, argu)
                && n.f6.accept(this, argu);
    }

    @Override
    public Boolean visit(WhileStatement n, TypeEnv argu) {
        return Util.checkExpr(n.f2, Prim.BOOL, argu) && n.f4.accept(this, argu);
    }

    @Override
    public Boolean visit(PrintStatement n, TypeEnv argu) {
        return Util.checkExpr(n.f2, Prim.INT, argu);
    }
}

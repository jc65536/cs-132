import cs132.IR.sparrow.Instruction;
import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

public class StmtVisitor extends GJDepthFirst<List<Instruction>, TypeEnv> {
    @Override
    public List<Instruction> visit(Statement n, TypeEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public List<Instruction> visit(Block n, TypeEnv argu) {
        return n.f1.accept(new ListVisitor<>(this), argu).flatMap(l -> l);
    }
}

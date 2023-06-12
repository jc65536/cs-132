import cs132.IR.sparrowv.*;
import cs132.IR.sparrowv.visitor.*;
import cs132.IR.token.*;

public class IdVisitor implements ArgRetVisitor<List<Identifier>, List<Identifier>> {
    @Override
    public List<Identifier> visit(Program arg0, List<Identifier> arg1) {
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public List<Identifier> visit(FunctionDecl arg0, List<Identifier> arg1) {
        final var params = List.fromJavaList(arg0.formalParameters);
        return arg0.block.accept(this, arg1).join(params)
                .unique((a, b) -> a.toString().equals(b.toString()));
    }

    @Override
    public List<Identifier> visit(Block arg0, List<Identifier> arg1) {
        return List.fromJavaList(arg0.instructions)
                .fold(arg1, (acc, ins) -> ins.accept(this, acc));
    }

    @Override
    public List<Identifier> visit(LabelInstr arg0, List<Identifier> arg1) {
        return arg1;
    }

    @Override
    public List<Identifier> visit(Move_Reg_Integer arg0, List<Identifier> arg1) {
        return arg1;
    }

    @Override
    public List<Identifier> visit(Move_Reg_FuncName arg0, List<Identifier> arg1) {
        return arg1;
    }

    @Override
    public List<Identifier> visit(Add arg0, List<Identifier> arg1) {
        return arg1;
    }

    @Override
    public List<Identifier> visit(Subtract arg0, List<Identifier> arg1) {
        return arg1;
    }

    @Override
    public List<Identifier> visit(Multiply arg0, List<Identifier> arg1) {
        return arg1;
    }

    @Override
    public List<Identifier> visit(LessThan arg0, List<Identifier> arg1) {
        return arg1;
    }

    @Override
    public List<Identifier> visit(Load arg0, List<Identifier> arg1) {
        return arg1;
    }

    @Override
    public List<Identifier> visit(Store arg0, List<Identifier> arg1) {
        return arg1;
    }

    @Override
    public List<Identifier> visit(Move_Reg_Reg arg0, List<Identifier> arg1) {
        return arg1;
    }

    @Override
    public List<Identifier> visit(Move_Id_Reg arg0, List<Identifier> arg1) {
        return arg1.cons(arg0.lhs);
    }

    @Override
    public List<Identifier> visit(Move_Reg_Id arg0, List<Identifier> arg1) {
        return arg1;
    }

    @Override
    public List<Identifier> visit(Alloc arg0, List<Identifier> arg1) {
        return arg1;
    }

    @Override
    public List<Identifier> visit(Print arg0, List<Identifier> arg1) {
        return arg1;
    }

    @Override
    public List<Identifier> visit(ErrorMessage arg0, List<Identifier> arg1) {
        return arg1;
    }

    @Override
    public List<Identifier> visit(Goto arg0, List<Identifier> arg1) {
        return arg1;
    }

    @Override
    public List<Identifier> visit(IfGoto arg0, List<Identifier> arg1) {
        return arg1;
    }

    @Override
    public List<Identifier> visit(Call arg0, List<Identifier> arg1) {
        return arg1;
    }
}

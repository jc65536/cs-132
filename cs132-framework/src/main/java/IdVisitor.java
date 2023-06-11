import cs132.IR.sparrowv.*;
import cs132.IR.sparrowv.visitor.ArgRetVisitor;
import cs132.IR.token.Identifier;

public class IdVisitor implements ArgRetVisitor<List<String>, List<String>> {
    @Override
    public List<String> visit(Program arg0, List<String> arg1) {
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public List<String> visit(FunctionDecl arg0, List<String> arg1) {
        final var params = List.fromJavaList(arg0.formalParameters).map(Identifier::toString);
        return arg0.block.accept(this, arg1).join(params).unique(String::equals);
    }

    @Override
    public List<String> visit(Block arg0, List<String> arg1) {
        return List.fromJavaList(arg0.instructions)
                .fold(arg1, (acc, ins) -> ins.accept(this, acc));
    }

    @Override
    public List<String> visit(LabelInstr arg0, List<String> arg1) {
        return arg1;
    }

    @Override
    public List<String> visit(Move_Reg_Integer arg0, List<String> arg1) {
        return arg1;
    }

    @Override
    public List<String> visit(Move_Reg_FuncName arg0, List<String> arg1) {
        return arg1;
    }

    @Override
    public List<String> visit(Add arg0, List<String> arg1) {
        return arg1;
    }

    @Override
    public List<String> visit(Subtract arg0, List<String> arg1) {
        return arg1;
    }

    @Override
    public List<String> visit(Multiply arg0, List<String> arg1) {
        return arg1;
    }

    @Override
    public List<String> visit(LessThan arg0, List<String> arg1) {
        return arg1;
    }

    @Override
    public List<String> visit(Load arg0, List<String> arg1) {
        return arg1;
    }

    @Override
    public List<String> visit(Store arg0, List<String> arg1) {
        return arg1;
    }

    @Override
    public List<String> visit(Move_Reg_Reg arg0, List<String> arg1) {
        return arg1;
    }

    @Override
    public List<String> visit(Move_Id_Reg arg0, List<String> arg1) {
        return arg1.cons(arg0.lhs.toString());
    }

    @Override
    public List<String> visit(Move_Reg_Id arg0, List<String> arg1) {
        return arg1;
    }

    @Override
    public List<String> visit(Alloc arg0, List<String> arg1) {
        return arg1;
    }

    @Override
    public List<String> visit(Print arg0, List<String> arg1) {
        return arg1;
    }

    @Override
    public List<String> visit(ErrorMessage arg0, List<String> arg1) {
        return arg1;
    }

    @Override
    public List<String> visit(Goto arg0, List<String> arg1) {
        return arg1;
    }

    @Override
    public List<String> visit(IfGoto arg0, List<String> arg1) {
        return arg1;
    }

    @Override
    public List<String> visit(Call arg0, List<String> arg1) {
        return arg1;
    }
}

import cs132.IR.sparrow.*;
import cs132.IR.sparrow.visitor.*;

public abstract class ARVisitor<A, R> implements ArgRetVisitor<A, R> {
    @Override
    public R visit(Program arg0, A arg1) {
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public R visit(FunctionDecl arg0, A arg1) {
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public R visit(Block arg0, A arg1) {
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public R visit(LabelInstr arg0, A arg1) {
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public R visit(Move_Id_Integer arg0, A arg1) {
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public R visit(Move_Id_FuncName arg0, A arg1) {
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public R visit(Add arg0, A arg1) {
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public R visit(Subtract arg0, A arg1) {
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public R visit(Multiply arg0, A arg1) {
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public R visit(LessThan arg0, A arg1) {
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public R visit(Load arg0, A arg1) {
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public R visit(Store arg0, A arg1) {
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public R visit(Move_Id_Id arg0, A arg1) {
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public R visit(Alloc arg0, A arg1) {
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public R visit(Print arg0, A arg1) {
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public R visit(ErrorMessage arg0, A arg1) {
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public R visit(Goto arg0, A arg1) {
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public R visit(IfGoto arg0, A arg1) {
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public R visit(Call arg0, A arg1) {
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }
}

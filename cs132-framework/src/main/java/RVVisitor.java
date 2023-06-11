import java.util.function.*;

import cs132.IR.sparrowv.*;
import cs132.IR.sparrowv.visitor.RetVisitor;

public class RVVisitor implements RetVisitor<Function<List<RVInstr>, List<RVInstr>>> {
    static Function<List<RVInstr>, List<RVInstr>> ident = x -> x;

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Program arg0) {
        return List.fromJavaList(arg0.funDecls)
            .fold(ident, (acc, fun) -> acc.andThen(fun.accept(this)));
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(FunctionDecl arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Block arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(LabelInstr arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Move_Reg_Integer arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Move_Reg_FuncName arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Add arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Subtract arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Multiply arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(LessThan arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Load arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Store arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Move_Reg_Reg arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Move_Id_Reg arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Move_Reg_Id arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Alloc arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Print arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(ErrorMessage arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Goto arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(IfGoto arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Call arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

}

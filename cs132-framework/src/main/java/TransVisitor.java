import java.util.function.Function;

import cs132.IR.sparrow.*;
import cs132.IR.sparrowv.Instruction;
import cs132.IR.sparrowv.Move_Id_Reg;
import cs132.IR.sparrowv.Move_Reg_FuncName;
import cs132.IR.sparrowv.Move_Reg_Id;
import cs132.IR.sparrowv.Move_Reg_Integer;
import cs132.IR.sparrowv.Move_Reg_Reg;
import cs132.IR.token.Identifier;
import cs132.IR.token.Register;

public class TransVisitor extends ARVisitor<FunctionInfo, Function<List<Instruction>, List<Instruction>>> {
    @Override
    public Function<List<Instruction>, List<Instruction>> visit(LabelInstr arg0, FunctionInfo arg1) {
        return tl -> tl.cons(new cs132.IR.sparrowv.LabelInstr(arg0.label));
    }

    @Override
    public Function<List<Instruction>, List<Instruction>> visit(Move_Id_Integer arg0, FunctionInfo arg1) {
        return arg1.regs.find(v -> v.a.nameEquals(arg0.lhs))
                .<Function<List<Instruction>, List<Instruction>>>map(t -> tl -> tl
                        .cons(new Move_Reg_Integer(t.b, arg0.rhs)))
                .orElse(tl -> tl
                        .cons(new Move_Reg_Integer(Regs.t0, arg0.rhs))
                        .cons(new Move_Id_Reg(arg0.lhs, Regs.t0)));
    }

    @Override
    public Function<List<Instruction>, List<Instruction>> visit(Move_Id_FuncName arg0, FunctionInfo arg1) {
        return arg1.regs.find(v -> v.a.nameEquals(arg0.lhs))
                .<Function<List<Instruction>, List<Instruction>>>map(t -> tl -> tl
                        .cons(new Move_Reg_FuncName(t.b, arg0.rhs)))
                .orElse(tl -> tl
                        .cons(new Move_Reg_FuncName(Regs.t0, arg0.rhs))
                        .cons(new Move_Id_Reg(arg0.lhs, Regs.t0)));
    }

    @Override
    public Function<List<Instruction>, List<Instruction>> visit(Add arg0, FunctionInfo arg1) {
        return binop(arg0.lhs, arg0.arg1, arg0.arg2,
                cs132.IR.sparrowv.Add::new, arg1);
    }

    @Override
    public Function<List<Instruction>, List<Instruction>> visit(Subtract arg0, FunctionInfo arg1) {
        return binop(arg0.lhs, arg0.arg1, arg0.arg2,
                cs132.IR.sparrowv.Subtract::new, arg1);
    }

    @Override
    public Function<List<Instruction>, List<Instruction>> visit(Multiply arg0, FunctionInfo arg1) {
        return binop(arg0.lhs, arg0.arg1, arg0.arg2,
                cs132.IR.sparrowv.Multiply::new, arg1);
    }

    @Override
    public Function<List<Instruction>, List<Instruction>> visit(LessThan arg0, FunctionInfo arg1) {
        return binop(arg0.lhs, arg0.arg1, arg0.arg2,
                cs132.IR.sparrowv.LessThan::new, arg1);
    }

    @Override
    public Function<List<Instruction>, List<Instruction>> visit(Load arg0, FunctionInfo arg1) {
        final var lhs = arg1.regs.find(v -> v.a.nameEquals(arg0.lhs));
        final var base = arg1.regs.find(v -> v.a.nameEquals(arg0.base));

        final var lhsReg = lhs.map(t -> t.b).orElse(Regs.t0);
        final var baseReg = base.map(t -> t.b).orElse(Regs.t1);

        return base.<Function<List<Instruction>, List<Instruction>>>map(u -> x -> x)
                .orElse(tr -> tr.cons(new Move_Reg_Id(Regs.t1, arg0.base)))
                .andThen(tr -> tr.cons(new cs132.IR.sparrowv.Load(lhsReg, baseReg, arg0.offset)))
                .andThen(lhs.<Function<List<Instruction>, List<Instruction>>>map(u -> x -> x)
                        .orElse(tr -> tr.cons(new Move_Id_Reg(arg0.lhs, Regs.t0))));
    }

    @Override
    public Function<List<Instruction>, List<Instruction>> visit(Store arg0, FunctionInfo arg1) {
        final var rhs = arg1.regs.find(v -> v.a.nameEquals(arg0.rhs));
        final var base = arg1.regs.find(v -> v.a.nameEquals(arg0.base));

        final var rhsReg = rhs.map(t -> t.b).orElse(Regs.t0);
        final var baseReg = base.map(t -> t.b).orElse(Regs.t1);

        return base.<Function<List<Instruction>, List<Instruction>>>map(u -> x -> x)
                .orElse(tr -> tr.cons(new Move_Reg_Id(Regs.t1, arg0.base)))
                .andThen(rhs.<Function<List<Instruction>, List<Instruction>>>map(u -> x -> x)
                        .orElse(tr -> tr.cons(new Move_Reg_Id(Regs.t0, arg0.rhs))))
                .andThen(tr -> tr.cons(new cs132.IR.sparrowv.Store(baseReg, arg0.offset, rhsReg)));
    }

    @Override
    public Function<List<Instruction>, List<Instruction>> visit(Move_Id_Id arg0, FunctionInfo arg1) {
        final var lhs = arg1.regs.find(v -> v.a.nameEquals(arg0.lhs));
        final var rhs = arg1.regs.find(v -> v.a.nameEquals(arg0.rhs));

        final var lhsReg = lhs.map(t -> t.b).orElse(Regs.t0);
        final var rhsReg = rhs.map(t -> t.b).orElse(Regs.t0);

        return rhs.<Function<List<Instruction>, List<Instruction>>>map(u -> x -> x)
                .orElse(tr -> tr.cons(new Move_Reg_Id(rhsReg, arg0.rhs)))
                .andThen(tr -> tr.cons(new Move_Reg_Reg(lhsReg, rhsReg)))
                .andThen(lhs.<Function<List<Instruction>, List<Instruction>>>map(u -> x -> x)
                        .orElse(tr -> tr.cons(new Move_Id_Reg(arg0.lhs, lhsReg))));
    }

    @Override
    public Function<List<Instruction>, List<Instruction>> visit(Alloc arg0, FunctionInfo arg1) {
        final var lhs = arg1.regs.find(v -> v.a.nameEquals(arg0.lhs));
        final var size = arg1.regs.find(v -> v.a.nameEquals(arg0.size));

        final var lhsReg = lhs.map(t -> t.b).orElse(Regs.t0);
        final var sizeReg = size.map(t -> t.b).orElse(Regs.t0);

        return size.<Function<List<Instruction>, List<Instruction>>>map(u -> x -> x)
                .orElse(tr -> tr.cons(new Move_Reg_Id(sizeReg, arg0.size)))
                .andThen(tr -> tr.cons(new cs132.IR.sparrowv.Alloc(lhsReg, sizeReg)))
                .andThen(lhs.<Function<List<Instruction>, List<Instruction>>>map(u -> x -> x)
                        .orElse(tr -> tr.cons(new Move_Id_Reg(arg0.lhs, lhsReg))));
    }

    @Override
    public Function<List<Instruction>, List<Instruction>> visit(Print arg0, FunctionInfo arg1) {
        return arg1.regs.find(v -> v.a.nameEquals(arg0.content))
                .<Function<List<Instruction>, List<Instruction>>>map(t -> tl -> tl
                        .cons(new cs132.IR.sparrowv.Print(t.b)))
                .orElse(tl -> tl
                        .cons(new Move_Reg_Id(Regs.t0, arg0.content))
                        .cons(new cs132.IR.sparrowv.Print(Regs.t0)));
    }

    @Override
    public Function<List<Instruction>, List<Instruction>> visit(ErrorMessage arg0, FunctionInfo arg1) {
        return tr -> tr.cons(new cs132.IR.sparrowv.ErrorMessage(arg0.msg));
    }

    @Override
    public Function<List<Instruction>, List<Instruction>> visit(Goto arg0, FunctionInfo arg1) {
        return tr -> tr.cons(new cs132.IR.sparrowv.Goto(arg0.label));
    }

    @Override
    public Function<List<Instruction>, List<Instruction>> visit(IfGoto arg0, FunctionInfo arg1) {
        return arg1.regs.find(v -> v.a.nameEquals(arg0.condition))
                .<Function<List<Instruction>, List<Instruction>>>map(t -> tl -> tl
                        .cons(new cs132.IR.sparrowv.IfGoto(t.b, arg0.label)))
                .orElse(tl -> tl
                        .cons(new Move_Reg_Id(Regs.t0, arg0.condition))
                        .cons(new cs132.IR.sparrowv.IfGoto(Regs.t0, arg0.label)));
    }

    @Override
    public Function<List<Instruction>, List<Instruction>> visit(Call arg0, FunctionInfo arg1) {
        final var args = List.fromJavaList(arg0.args);
        final var zip = List.zip(args, Regs.argRegs);

        final var lhs = arg1.regs.find(v -> v.a.nameEquals(arg0.lhs));
        final var lhsReg = lhs.map(t -> t.b).orElse(Regs.t1);
        final var callee = arg1.regs.find(v -> v.a.nameEquals(arg0.callee));
        final var calleeReg = callee.map(t -> t.b).orElse(Regs.t0);

        final Function<List<Instruction>, List<Instruction>> callerSave = zip.a
                .map(t -> saveReg(t.b))::join;

        final Function<List<Instruction>, List<Instruction>> setRegArgs = zip.a
                .map(t -> arg1.regs.find(v -> v.a.nameEquals(t.a))
                        .<Instruction>map(t2 -> new Move_Reg_Reg(t.b, t2.b))
                        .orElse(new Move_Reg_Id(t.b, t.a)))::join;

        final var setMemArgs = zip.b.<Function<List<Instruction>, List<Instruction>>>fold(x -> x,
                (acc, id) -> acc.andThen(arg1.regs.find(v -> v.a.nameEquals(id))
                        .<Function<List<Instruction>, List<Instruction>>>map(
                                t -> tr -> tr.cons(new Move_Id_Reg(id, t.b)))
                        .orElse(x -> x)));

        final Function<List<Instruction>, List<Instruction>> callerRestore = zip.a
                .map(t -> restoreReg(t.b))::join;

        return callerSave.andThen(setRegArgs)
                .andThen(setMemArgs)
                .andThen(tr -> callee.map(u -> tr).orElseGet(() -> tr.cons(new Move_Reg_Id(calleeReg, arg0.callee))))
                .andThen(tr -> tr.cons(new cs132.IR.sparrowv.Call(lhsReg, calleeReg, zip.b.toJavaList())))
                .andThen(tr -> lhs.map(u -> tr).orElseGet(() -> tr.cons(new Move_Id_Reg(arg0.lhs, lhsReg))))
                .andThen(callerRestore);
    }

    static Function<List<Instruction>, List<Instruction>> binop(Identifier lhsId, Identifier op1Id, Identifier op2Id,
            F3<Register, Register, Register, Instruction> mkIns, FunctionInfo arg1) {
        final var lhs = arg1.regs.find(v -> v.a.nameEquals(lhsId));
        final var op1 = arg1.regs.find(v -> v.a.nameEquals(op1Id));
        final var op2 = arg1.regs.find(v -> v.a.nameEquals(op2Id));

        final var lhsReg = lhs.map(t -> t.b).orElse(Regs.t0);
        final var op1Reg = op1.map(t -> t.b).orElse(Regs.t0);
        final var op2Reg = op2.map(t -> t.b).orElse(Regs.t1);

        return op1.<Function<List<Instruction>, List<Instruction>>>map(u -> x -> x)
                .orElse(tr -> tr.cons(new Move_Reg_Id(Regs.t0, op1Id)))
                .andThen(op2.<Function<List<Instruction>, List<Instruction>>>map(u -> x -> x)
                        .orElse(tr -> tr.cons(new Move_Reg_Id(Regs.t1, op2Id))))
                .andThen(tr -> tr.cons(mkIns.apply(lhsReg, op1Reg, op2Reg)))
                .andThen(lhs.<Function<List<Instruction>, List<Instruction>>>map(u -> x -> x)
                        .orElse(tr -> tr.cons(new Move_Id_Reg(lhsId, Regs.t0))));
    }

    static Instruction saveReg(Register reg) {
        return new Move_Id_Reg(new Identifier("__save__" + reg.toString()), reg);
    }

    static Instruction restoreReg(Register reg) {
        return new Move_Reg_Id(reg, new Identifier("__save__" + reg.toString()));
    }
}

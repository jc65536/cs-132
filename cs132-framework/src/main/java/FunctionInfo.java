import java.util.function.Function;

import cs132.IR.sparrowv.*;
import cs132.IR.token.FunctionName;
import cs132.IR.token.Identifier;
import cs132.IR.token.Register;

public class FunctionInfo {
    final List<FunctionInfo> allFns;
    final FunctionName functionName;
    final List<T2<Var, Register>> regParams;
    final List<Var> memParams;
    final List<T2<Var, Register>> regLocals;
    final List<Var> memLocals;

    final List<T2<Var, Register>> regs;
    final List<Var> mems;

    final List<CFNode> body;
    final Var retId;

    public FunctionInfo(FunctionName functionName, List<Var> params, RegAlloc alloc, List<CFNode> body, Var retId, List<FunctionInfo> allFns) {
        this.functionName = functionName;
        this.regLocals = alloc.regs;
        this.memLocals = alloc.mems;

        final var zip = List.zip(params, Regs.argRegs);

        this.regParams = zip.a;
        this.memParams = zip.b;

        this.regs = regLocals.join(regParams);
        this.mems = memLocals.join(memParams);

        this.body = body;
        this.retId = retId;
        this.allFns = allFns;
    }

    boolean nameEquals(FunctionInfo other) {
        return functionName.toString().equals(other.functionName.toString());
    }

    FunctionDecl translate() {
        final Function<List<Instruction>, List<Instruction>> calleeSave = regLocals
                .map(t -> TransVisitor.saveReg(t.b))::join;

        final Function<List<Instruction>, List<Instruction>> transBody = tr -> body
                .fold(tr, (acc, node) -> node.ins.accept(new TransVisitor(), this).apply(S2SV.DEBUG >= 1
                ? acc.cons(new Move_Id_Reg(
                    new Identifier("________" + node.ins.toString()
                    .replace(' ', '_')
                    .replace("=", "_eq_")
                    .replace("+", "_p_")
                    .replace("-", "_m_")
                    .replace("*", "_t_")
                    .replace("<", "_lt_")
                    .replace("[", "_l_")
                    .replace("]", "_r_")
                    .replace(":", "")
                    .replace("@", "_fn_")
                    .replace('(', '_')
                    .replace(')', '_')), Regs.t0))
                : acc));

        final Function<List<Instruction>, List<Instruction>> calleeRestore = regLocals
                .map(t -> TransVisitor.restoreReg(t.b))::join;

        final var ins = calleeSave.andThen(transBody)
                .andThen(tr -> regs.find(v -> v.a.nameEquals(retId))
                        .map(t -> tr.cons(new Move_Id_Reg(retId.id, t.b)))
                        .orElse(tr))
                .andThen(calleeRestore)
                .apply(List.nul())
                .reverse()
                .toJavaList();

        final var block = new Block(ins, retId.id);

        return new FunctionDecl(functionName,
                memParams.map(v -> v.id).toJavaList(), block);
    }

    @Override
    public String toString() {
        return String.format("Reg params: %s\nMem params: %s\nReg locals: %s\nMem locals: %s\n",
                regParams.map(t -> String.format("%s -> %s", t.a, t.b)).strJoin(", "),
                memParams.strJoin(", "),
                regLocals.map(t -> String.format("%s -> %s", t.a, t.b)).strJoin(", "),
                memLocals.strJoin(", "));
    }
}

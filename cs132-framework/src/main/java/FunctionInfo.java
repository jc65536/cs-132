import java.util.Optional;
import java.util.function.Function;

import cs132.IR.sparrowv.*;
import cs132.IR.token.FunctionName;
import cs132.IR.token.Identifier;
import cs132.IR.token.Register;

public class FunctionInfo {
    final List<FunctionInfo> allFns;
    final FunctionName functionName;

    final List<T2<Identifier, Register>> regParams;
    final List<Identifier> memParams;

    final List<T2<Identifier, Register>> regLocals;
    final List<T2<Identifier, Register>> allRegs;

    final List<CFNode> body;
    final Identifier retId;

    final List<Identifier> dead;

    public FunctionInfo(FunctionName functionName, List<Identifier> params, RegAlloc alloc, List<CFNode> body,
            Identifier retId,
            List<FunctionInfo> allFns,
            List<Identifier> dead) {
        this.functionName = functionName;
        this.regLocals = alloc.regs;

        final var zip = List.zip(params, Regs.argRegs);

        this.regParams = zip.a;
        this.memParams = zip.b;

        this.allRegs = regLocals.join(regParams);

        this.body = body;
        this.retId = retId;
        this.allFns = allFns;

        this.dead = dead;
    }

    FunctionDecl translate() {
        final Function<List<Instruction>, List<Instruction>> calleeSave = regLocals
                .map(t -> t.b)
                .unique(Util::nameEq)
                .map(TransVisitor::saveReg)::join;

        final Function<List<Instruction>, List<Instruction>> transBody = body
                .fold(Function.identity(), (acc, node) -> acc
                        .andThen(tr -> S2SV.DEBUG >= 1 ? tr.cons(Util.comment(node.ins)) : tr)
                        .andThen(node.translate(this)));

        final Function<List<Instruction>, List<Instruction>> calleeRestore = regLocals
                .map(t -> t.b)
                .unique(Util::nameEq)
                .map(TransVisitor::restoreReg)::join;

        final var ins = calleeSave.andThen(transBody)
                .andThen(tr -> allRegs.find(v -> Util.nameEq(v.a, retId))
                        .map(t -> tr.cons(new Move_Id_Reg(retId, t.b)))
                        .orElse(tr))
                .andThen(calleeRestore)
                .apply(List.nul())
                .reverse()
                .toJavaList();

        final var block = new Block(ins, retId);

        return new FunctionDecl(functionName,
                memParams.toJavaList(), block);
    }

    @Override
    public String toString() {
        return String.format("Reg params: %s\nMem params: %s\nReg locals: %s\n",
                regParams.map(t -> String.format("%s -> %s", t.a, t.b)).strJoin(", "),
                memParams.strJoin(", "),
                regLocals.map(t -> String.format("%s -> %s", t.a, t.b)).strJoin(", "));
    }

    boolean isDead(Identifier id) {
        return dead.exists(v -> Util.nameEq(v, id));
    }

    Optional<T2<Identifier, Register>> regLookup(Identifier id) {
        return allRegs.find(t -> Util.nameEq(t.a, id));
    }
}

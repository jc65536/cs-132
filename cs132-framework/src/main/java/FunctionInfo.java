import java.util.Optional;
import java.util.function.Function;

import cs132.IR.sparrowv.*;
import cs132.IR.token.FunctionName;
import cs132.IR.token.Identifier;
import cs132.IR.token.Register;

public class FunctionInfo {
    final FunctionName functionName;

    final List<T2<Identifier, Register>> regParams;
    final List<Identifier> memParams;

    final List<T2<Identifier, Register>> regLocals;
    final List<T2<Identifier, Register>> usedRegs;

    final List<NodeInOut> body;
    final Identifier retId;

    final List<Identifier> dead;

    public FunctionInfo(FunctionName functionName,
            List<Identifier> params,
            RegAlloc alloc,
            List<NodeInOut> body,
            Identifier retId,
            List<Identifier> dead) {
        this.functionName = functionName;
        this.regLocals = alloc.regs;

        final var zip = List.zip(params, Regs.argRegs);

        this.regParams = zip.a;
        this.memParams = zip.b;

        this.usedRegs = regLocals.join(regParams);

        this.body = body;
        this.retId = retId;

        this.dead = dead;
    }

    FunctionDecl translate(boolean saveRegs) {
        final Function<List<Instruction>, List<Instruction>> calleeSave = saveRegs
                ? regLocals.map(t -> t.b)
                        .unique(Util::nameEq)
                        .map(TransVisitor::saveReg)::join
                : x -> x;

        final var transBody = body
                .fold(TransVisitor.ident, (acc, nio) -> acc
                        .andThen(tr -> S2SV.DEBUG >= 1 ? tr.cons(Util.comment(nio.node.ins)) : tr)
                        .andThen(nio.node.translate(this)));

        final Function<List<Instruction>, List<Instruction>> calleeRestore = saveRegs
                ? regLocals.map(t -> t.b)
                        .unique(Util::nameEq)
                        .map(TransVisitor::restoreReg)::join
                : x -> x;

        final var ins = calleeSave.andThen(transBody)
                .andThen(tr -> regLookup(retId)
                        .map(r -> tr.cons(new Move_Id_Reg(retId, r)))
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

    Optional<Register> regLookup(Identifier id) {
        return usedRegs.find(t -> Util.nameEq(t.a, id)).map(t -> t.b);
    }
}

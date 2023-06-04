import cs132.IR.sparrow.Instruction;
import cs132.IR.sparrow.LabelInstr;
import cs132.IR.token.Label;
import cs132.IR.token.Register;

public class LiveRange {
    final Var var;
    final int begin;
    Instruction beginIns;
    final int end;
    Instruction endIns;

    LiveRange(Var var, List<NodeInOut> cfGraph) {
        this.var = var;
        this.beginIns = this.endIns = new LabelInstr(new Label("none"));
        this.begin = cfGraph.firstIndex(n -> {
            final var b = n.out.exists(var::nameEquals);
            this.beginIns = n.node.ins;
            return b;
        }).orElse(-1);
        this.end = cfGraph.count() - cfGraph.reverse().firstIndex(n -> {
            final var b = n.in.exists(var::nameEquals);
            this.endIns = n.node.ins;
            return b;
        }).orElse(-1);
    }

    @Override
    public String toString() {
        return String.format("  %s: %d (%s) -- %d (%s)\n", var, begin, beginIns, end, endIns);
    }
}

class RegAlloc {
    final List<T2<Var, Register>> regs;
    final List<Var> mems;

    final List<LiveRange> active;
    final List<Register> freeRegs;

    static final Register tmp = new Register("t0");

    RegAlloc(List<T2<Var, Register>> reg, List<Var> mem, List<LiveRange> active, List<Register> freeRegs) {
        this.regs = reg;
        this.mems = mem;
        this.active = active;
        this.freeRegs = freeRegs;
    }

    RegAlloc() {
        this(List.nul(), List.nul(), List.nul(), Regs.all);
    }

    RegAlloc cons(T2<Var, Register> v) {
        return new RegAlloc(regs.cons(v), mems, active, freeRegs);
    }

    RegAlloc cons(Var v) {
        return new RegAlloc(regs, mems.cons(v), active, freeRegs);
    }

    RegAlloc cons(LiveRange v) {
        return new RegAlloc(regs, mems, active.cons(v), freeRegs);
    }

    RegAlloc cons(Register v) {
        return new RegAlloc(regs, mems, active, freeRegs.cons(v));
    }

    RegAlloc setFreeRegs(List<Register> freeRegs) {
        return new RegAlloc(regs, mems, active, freeRegs);
    }

    @Override
    public String toString() {
        return String.format("Active: %s\nFree regs: %s\nRegs: %s\nMems: %s\n",
            active.map(r -> r.var).strJoin(", "),
            freeRegs.strJoin(", "),
            regs.map(t -> String.format("%s -> %s", t.a, t.b)).strJoin(", "),
            mems.strJoin(", "));
    }
}

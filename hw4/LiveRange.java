import cs132.IR.token.*;
import cs132.IR.token.Register;

public class LiveRange {
    final Identifier id;
    final int begin;
    final int end;

    final List<NodeInOut> stretch;

    final double useDensity;

    LiveRange(Identifier var, List<NodeInOut> cfGraph) {
        this.id = var;
        this.begin = cfGraph.firstIndex(n -> n.out
                .exists(v -> Util.nameEq(id, v))).orElse(-1);
        
        final var count = cfGraph.count();

        this.end = count - 1 - cfGraph.reverse().firstIndex(n -> n.in
                .exists(v -> Util.nameEq(id, v))).orElse(count + 1);
        
        stretch = cfGraph.foldI(List.<NodeInOut>nul(), (acc, nio, i) -> {
            if (begin <= i && i < end) {
                return acc.cons(nio);
            } else {
                return acc;
            }
        }).reverse();

        useDensity = stretch.fold(0, (acc, nio) -> {
            return acc + nio.node.use.filter(i -> Util.nameEq(i, id)).count();
        }) / ((double) this.end - this.begin);
    }

    @Override
    public String toString() {
        return String.format("  %s: %d -- %d\n", id, begin, end);
    }
}

class RegAlloc {
    final List<T2<Identifier, Register>> regs;
    final List<Identifier> mems;

    final List<LiveRange> active;
    final List<Register> freeRegs;

    RegAlloc(List<T2<Identifier, Register>> reg,
            List<Identifier> mem,
            List<LiveRange> active,
            List<Register> freeRegs) {
        this.regs = reg;
        this.mems = mem;
        this.active = active;
        this.freeRegs = freeRegs;
    }

    RegAlloc(List<Register> freeRegs) {
        this(List.nul(), List.nul(), List.nul(), freeRegs);
    }

    RegAlloc cons(T2<Identifier, Register> v) {
        return new RegAlloc(regs.cons(v), mems, active, freeRegs);
    }

    RegAlloc cons(Identifier v) {
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
                active.map(r -> r.id).strJoin(", "),
                freeRegs.strJoin(", "),
                regs.map(t -> String.format("%s -> %s", t.a, t.b)).strJoin(", "),
                mems.strJoin(", "));
    }
}

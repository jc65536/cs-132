import java.util.Optional;
import java.util.function.*;

import cs132.IR.sparrow.Instruction;
import cs132.IR.sparrow.LabelInstr;
import cs132.IR.token.Identifier;
import cs132.IR.token.Label;

class Var {
    final Identifier id;
    final CFNode lastUse;

    Var(Identifier id, CFNode lastUse) {
        this.id = id;
        this.lastUse = lastUse;
    }

    boolean nameEquals(Var other) {
        return id.toString().equals(other.id.toString());
    }
}

public class CFNode {
    final Instruction ins;
    final List<? extends CFNode> succ;
    final List<Var> def;
    final List<Var> use;

    final List<Var> in;
    final List<Var> out;

    CFNode(Instruction ins, List<? extends CFNode> succ, List<Identifier> defIds, List<Identifier> useIds, List<CFNode> rest) {
        this.ins = ins;
        this.succ = succ;
        def = defIds.map(id -> new Var(id, this)).unique(Var::nameEquals);
        use = useIds.map(id -> new Var(id, this)).unique(Var::nameEquals);

        out = new List<>(() -> );
        in = use.join(out.filter(v -> !def.exists(v::nameEquals))).unique(Var::nameEquals);
    }

    static final Optional<Pair<Var>> f(List<? extends CFNode> succ) {
        succ.head()
            .map(n -> n.in.head().or())
    }

    @Override
    public String toString() {
        final BiFunction<String, Var, String> commaJoin = (acc, v) -> acc + v.id + ", ";
        return String.format("====\nNode: %s\t(%s)\nSuccessors: %s\nDef: %s\nUse: %s\n",
                ins,
                this.hashCode(),
                succ.fold("", (acc, s) -> acc + s.hashCode() + ", "),
                def.fold("", commaJoin),
                use.fold("", commaJoin));
    }
}

class LabelNode extends CFNode {
    final Label label;

    LabelNode(LabelInstr ins, List<CFNode> succ, List<CFNode> rest) {
        super(ins, succ, List.nul(), List.nul(), rest);
        this.label = ins.label;
    }

    boolean equalsLabel(Label l) {
        return label.toString().equals(l.toString());
    }
}

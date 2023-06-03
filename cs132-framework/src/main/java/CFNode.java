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

    CFNode(Instruction ins, List<? extends CFNode> succ, List<Identifier> defIds, List<Identifier> useIds,
            List<CFNode> rest) {
        this.ins = ins;
        this.succ = succ;
        def = defIds.map(id -> new Var(id, this)).unique(Var::nameEquals);
        use = useIds.map(id -> new Var(id, this)).unique(Var::nameEquals);
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

class NodeInOut {
    final CFNode node;
    final List<Var> in;
    final List<Var> out;

    NodeInOut(CFNode node, List<Var> in, List<Var> out) {
        this.node = node;
        this.in = in;
        this.out = out;
    }

    @Override
    public String toString() {
        return String.format("%s%s\n%s\n", node,
                in.fold("In: ", (acc, s) -> acc + s.id + ", "),
                out.fold("Out: ", (acc, s) -> acc + s.id + ", "));
    }
}

import java.util.Optional;
import java.util.function.*;

import cs132.IR.sparrow.Instruction;
import cs132.IR.sparrow.LabelInstr;
import cs132.IR.token.Identifier;
import cs132.IR.token.Label;

class Var {
    final Identifier id;

    Var(Identifier id) {
        this.id = id;
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

    CFNode(Instruction ins, List<? extends CFNode> succ, List<Identifier> def, List<Identifier> use) {
        this.ins = ins;
        this.succ = succ;
        this.def = def.map(Var::new).unique(Var::nameEquals);
        this.use = use.map(Var::new).unique(Var::nameEquals);


    }

    @Override
    public String toString() {
        final BiFunction<String, Var, String> commaJoin = (acc, v) -> acc + v.id + ", ";
        return String.format("====\nNode: %s\t(%s)\nSuccessors: %s\nDef: %s\nUse: %s\nIn: %s\nOut: %s\n",
                ins,
                this.hashCode(),
                succ.fold("", (acc, s) -> acc + s.hashCode() + ", "),
                def.fold("", commaJoin),
                use.fold("", commaJoin),
                in.fold("", commaJoin),
                out.fold("", commaJoin));
    }
}

class LabelNode extends CFNode {
    final Label label;

    LabelNode(LabelInstr ins, List<CFNode> succ) {
        super(ins, succ, List.nul(), List.nul());
        this.label = ins.label;
    }

    boolean equalsLabel(Label l) {
        return label.toString().equals(l.toString());
    }
}

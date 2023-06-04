import java.util.Optional;
import java.util.function.*;

import cs132.IR.sparrow.Instruction;
import cs132.IR.sparrow.LabelInstr;
import cs132.IR.token.Identifier;
import cs132.IR.token.Label;

class ReturnNode extends CFNode {
    ReturnNode(Identifier retId) {
        super(new LabelInstr(new Label("ret")), List.nul(), List.nul(), List.of(retId), List.nul());
    }
}

class ParamsNode extends CFNode {
    ParamsNode(List<Identifier> defIds, CFNode succ) {
        super(new LabelInstr(new Label("params")), List.of(succ), defIds, List.nul(), List.nul());
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
        def = defIds.map(Var::new).unique(Var::nameEquals);
        use = useIds.map(Var::new).unique(Var::nameEquals);
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

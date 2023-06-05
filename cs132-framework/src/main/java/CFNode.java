import java.util.function.*;

import cs132.IR.sparrow.*;
import cs132.IR.sparrowv.Instruction;
import cs132.IR.token.*;

class ReturnNode extends CFNode {
    ReturnNode(Identifier retId) {
        super(new LabelInstr(new Label("ret")), List.nul(), List.nul(), List.of(retId), List.nul());
    }

    @Override
    Function<List<Instruction>, List<Instruction>> translate(FunctionInfo fi) {
        return x -> x;
    }
}

class ParamsNode extends CFNode {
    ParamsNode(List<Identifier> defIds, CFNode succ) {
        super(new LabelInstr(new Label("params")), List.of(succ), defIds, List.nul(), List.nul());
    }

    @Override
    Function<List<Instruction>, List<Instruction>> translate(FunctionInfo fi) {
        return x -> x;
    }
}

public class CFNode {
    final cs132.IR.sparrow.Instruction ins;
    final List<? extends CFNode> succ;
    final List<Identifier> def;
    final List<Identifier> use;

    CFNode(cs132.IR.sparrow.Instruction ins, List<? extends CFNode> succ, List<Identifier> defIds, List<Identifier> useIds,
            List<CFNode> rest) {
        this.ins = ins;
        this.succ = succ;
        def = defIds.unique(Util::nameEq);
        use = useIds.unique(Util::nameEq);
    }

    Function<List<Instruction>, List<Instruction>> translate(FunctionInfo fi) {
        return ins.accept(new TransVisitor(), fi);
    }
}

class LabelNode extends CFNode {
    final Label label;

    LabelNode(LabelInstr ins, List<CFNode> succ, List<CFNode> rest) {
        super(ins, succ, List.nul(), List.nul(), rest);
        this.label = ins.label;
    }
}

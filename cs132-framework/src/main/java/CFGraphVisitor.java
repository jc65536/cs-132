import java.util.*;
import java.util.function.*;

import cs132.IR.sparrow.*;
import cs132.IR.token.Identifier;

public class CFGraphVisitor extends ARVisitor<List<LabelNode>, T2<List<CFNode>, List<LabelNode>>> {
    @Override
    public T2<List<CFNode>, List<LabelNode>> visit(FunctionDecl arg0, List<LabelNode> arg1) {
        return arg0.block.accept(this, arg1);
    }

    @Override
    public T2<List<CFNode>, List<LabelNode>> visit(Block arg0, List<LabelNode> arg1) {
        return List.fromJavaList(arg0.instructions)
                .map(ins -> ins.accept(new CFNodeVisitor(), arg1))
                .reverse()
                .fold(new T2<>(List.<CFNode>nul(), List.<LabelNode>nul()),
                        (acc, mkNode) -> acc.then(nodes -> labels -> mkNode.apply(nodes.head(), labels)
                                .then(node -> newLabels -> new T2<>(nodes.cons(node), newLabels))));
    }
}

class CFNodeVisitor
        extends ARVisitor<List<LabelNode>, BiFunction<Optional<CFNode>, List<LabelNode>, T2<CFNode, List<LabelNode>>>> {
    @Override
    public BiFunction<Optional<CFNode>, List<LabelNode>, T2<CFNode, List<LabelNode>>> visit(LabelInstr arg0,
            List<LabelNode> arg1) {
        return (next, labels) -> {
            final var node = new LabelNode(arg0, List.ofOpt(next));
            return new T2<>(node, labels.cons(node));
        };
    }

    @Override
    public BiFunction<Optional<CFNode>, List<LabelNode>, T2<CFNode, List<LabelNode>>> visit(Move_Id_Integer arg0,
            List<LabelNode> arg1) {
        return defuse(arg0, List.of(arg0.lhs), List.nul(), arg1);
    }

    @Override
    public BiFunction<Optional<CFNode>, List<LabelNode>, T2<CFNode, List<LabelNode>>> visit(Move_Id_FuncName arg0,
            List<LabelNode> arg1) {
        return defuse(arg0, List.of(arg0.lhs), List.nul(), arg1);
    }

    @Override
    public BiFunction<Optional<CFNode>, List<LabelNode>, T2<CFNode, List<LabelNode>>> visit(Add arg0,
            List<LabelNode> arg1) {
        return defuse(arg0, List.of(arg0.lhs), List.of(arg0.arg1).cons(arg0.arg2), arg1);
    }

    @Override
    public BiFunction<Optional<CFNode>, List<LabelNode>, T2<CFNode, List<LabelNode>>> visit(Subtract arg0,
            List<LabelNode> arg1) {
        return defuse(arg0, List.of(arg0.lhs), List.of(arg0.arg1).cons(arg0.arg2), arg1);
    }

    @Override
    public BiFunction<Optional<CFNode>, List<LabelNode>, T2<CFNode, List<LabelNode>>> visit(Multiply arg0,
            List<LabelNode> arg1) {
        return defuse(arg0, List.of(arg0.lhs), List.of(arg0.arg1).cons(arg0.arg2), arg1);
    }

    @Override
    public BiFunction<Optional<CFNode>, List<LabelNode>, T2<CFNode, List<LabelNode>>> visit(LessThan arg0,
            List<LabelNode> arg1) {
        return defuse(arg0, List.of(arg0.lhs), List.of(arg0.arg1).cons(arg0.arg2), arg1);
    }

    @Override
    public BiFunction<Optional<CFNode>, List<LabelNode>, T2<CFNode, List<LabelNode>>> visit(Load arg0,
            List<LabelNode> arg1) {
        return defuse(arg0, List.of(arg0.lhs), List.of(arg0.base), arg1);
    }

    @Override
    public BiFunction<Optional<CFNode>, List<LabelNode>, T2<CFNode, List<LabelNode>>> visit(Store arg0,
            List<LabelNode> arg1) {
        return defuse(arg0, List.nul(), List.of(arg0.base).cons(arg0.rhs), arg1);
    }

    @Override
    public BiFunction<Optional<CFNode>, List<LabelNode>, T2<CFNode, List<LabelNode>>> visit(Move_Id_Id arg0,
            List<LabelNode> arg1) {
        return defuse(arg0, List.of(arg0.lhs), List.of(arg0.rhs), arg1);
    }

    @Override
    public BiFunction<Optional<CFNode>, List<LabelNode>, T2<CFNode, List<LabelNode>>> visit(Alloc arg0,
            List<LabelNode> arg1) {
        return defuse(arg0, List.of(arg0.lhs), List.of(arg0.size), arg1);
    }

    @Override
    public BiFunction<Optional<CFNode>, List<LabelNode>, T2<CFNode, List<LabelNode>>> visit(Print arg0,
            List<LabelNode> arg1) {
        return defuse(arg0, List.nul(), List.of(arg0.content), arg1);
    }

    @Override
    public BiFunction<Optional<CFNode>, List<LabelNode>, T2<CFNode, List<LabelNode>>> visit(ErrorMessage arg0,
            List<LabelNode> arg1) {
        return (next, labels) -> new T2<>(new CFNode(arg0, List.nul(), List.nul(), List.nul()), labels);
    }

    @Override
    public BiFunction<Optional<CFNode>, List<LabelNode>, T2<CFNode, List<LabelNode>>> visit(Goto arg0,
            List<LabelNode> arg1) {
        return (next, labels) -> new T2<>(new CFNode(arg0, arg1.filter(lNode -> lNode.equalsLabel(arg0.label)),
                List.nul(), List.nul()), labels);
    }

    @Override
    public BiFunction<Optional<CFNode>, List<LabelNode>, T2<CFNode, List<LabelNode>>> visit(IfGoto arg0,
            List<LabelNode> arg1) {
        return (next, labels) -> new T2<>(
                new CFNode(arg0, List.ofOpt(next).join(arg1.filter(lNode -> lNode.equalsLabel(arg0.label))),
                        List.nul(), List.of(arg0.condition)),
                labels);
    }

    @Override
    public BiFunction<Optional<CFNode>, List<LabelNode>, T2<CFNode, List<LabelNode>>> visit(Call arg0,
            List<LabelNode> arg1) {
        return defuse(arg0, List.of(arg0.lhs), List.fromJavaList(arg0.args).cons(arg0.callee), arg1);
    }

    static BiFunction<Optional<CFNode>, List<LabelNode>, T2<CFNode, List<LabelNode>>> defuse(Instruction ins,
            List<Identifier> defs,
            List<Identifier> uses,
            List<LabelNode> allLabels) {
        return (next, labels) -> new T2<>(new CFNode(ins, List.ofOpt(next), defs, uses), labels);
    }
}

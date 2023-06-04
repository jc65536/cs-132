import java.util.function.*;

import cs132.IR.SparrowParser;
import cs132.IR.sparrowv.Program;
import cs132.IR.visitor.SparrowConstructor;

public class S2SV {
    static final int DEBUG = 0;

    static List<NodeInOut> fp(int inOutTotal, List<NodeInOut> curr) {
        final var nextRec = curr.fold(new T2<>(0, List.<NodeInOut>nul()), (acc, nio) -> acc.then(tot -> list -> {
            final var newIn = nio.node.use.join(nio.out.filter(v -> !nio.node.def.exists(v::nameEquals)))
                    .unique(Var::nameEquals);
            final var newOut = nio.node.succ.flatMap(s -> curr.find(t -> t.node == s).get().in).unique(Var::nameEquals);
            final var term = newIn.count() + newOut.count();
            return new T2<>(tot + term, list.cons(new NodeInOut(nio.node, newIn, newOut)));
        }));
        if (nextRec.a != inOutTotal) {
            return fp(nextRec.a, nextRec.b.reverse());
        } else {
            return curr;
        }
    }

    static RegAlloc linScanRegAlloc(List<LiveRange> ranges, RegAlloc state) {
        if (DEBUG == 4) {
            System.out.printf("****\n%s", state);
        }

        return ranges.get()
                .map(p -> {
                    final var i = p.val;
                    final var state2 = expOldInts(i, state);
                    if (state2.freeRegs.count() == 0) {
                        return linScanRegAlloc(p.next, spillAtInt(i, state2));
                    } else {
                        final var pair = state2.freeRegs.get().get();
                        return linScanRegAlloc(p.next, state2.setFreeRegs(pair.next)
                                .cons(new T2<>(i.var, pair.val))
                                .cons(i));
                    }
                })
                .orElse(state);
    }

    static RegAlloc expOldInts(LiveRange i, RegAlloc state) {
        return state.active.fold(new RegAlloc(state.regs, state.mems, List.nul(), state.freeRegs),
                (acc, lrange) -> {
                    if (lrange.end > i.begin) {
                        return acc.cons(lrange);
                    } else {
                        return acc.cons(state.regs.find(t -> t.a == lrange.var)
                                .get().b);
                    }
                });
    }

    static RegAlloc spillAtInt(LiveRange i, RegAlloc state) {
        final var spill = state.active.max((r1, r2) -> r1.end - r2.end).get();
        if (spill.end > i.end) {
            final var newReg = state.regs.map(t -> t.a == spill.var ? new T2<>(i.var, t.b) : t);
            final var newMem = state.mems.cons(spill.var);
            final var newActive = state.active.map(r -> r == spill ? i : r);
            return new RegAlloc(newReg, newMem, newActive, state.freeRegs);
        } else {
            return state.cons(i.var);
        }
    }

    public static void main(String[] args) throws Exception {
        new SparrowParser(System.in);
        final var ctor = new SparrowConstructor();
        SparrowParser.Program().accept(ctor);
        final var prgm = ctor.getProgram();
        final var fns = List.fromJavaList(prgm.funDecls);

        final var functionInfos = new Lazy<List<FunctionInfo>>(z -> fns.map(fn -> {
            final var cfGraph_ = new Lazy<T2<List<CFNode>, List<LabelNode>>>(
                    zz -> fn.accept(new CFGraphVisitor(), T2.unwrap(zz))).get().a;
            final var params = List.fromJavaList(fn.formalParameters).map(Var::new);

            final var cfGraph = cfGraph_
                    .cons(new ParamsNode(List.fromJavaList(fn.formalParameters), cfGraph_.head().get()));

            final var locals = cfGraph.flatMap(n -> n.def.join(n.use))
                    .filter(u -> !params.exists(u::nameEquals))
                    .unique(Var::nameEquals);

            if (DEBUG >= 2) {
                System.out.println("--------");
                System.out.println("Function " + fn.functionName);
                System.out.println("Params: " + params.strJoin(", "));
                System.out.println("Locals: " + locals.strJoin(", "));
            }

            final var k = cfGraph.map(n -> new NodeInOut(n, List.nul(), List.nul()));

            final var fixed = fp(0, k);

            if (DEBUG >= 3) {
                System.out.println("BEGIN CFGRAPH");
                System.out.print(fixed.strJoin(""));
                System.out.println("END CFGRAPH");
            }

            final var liveRanges = locals.map(v -> new LiveRange(v, fixed))
                    .sort((r1, r2) -> r1.begin - r2.begin);

            if (DEBUG >= 2) {
                System.out.printf("Live ranges:\n%s", liveRanges.strJoin(""));
            }

            final var alloc = linScanRegAlloc(liveRanges, new RegAlloc());

            final var check = alloc.regs.forAll(t -> {
                final var range = liveRanges.find(r -> r.var.nameEquals(t.a)).get();
                final var sameReg = alloc.regs.filter(t2 -> t2.b == t.b)
                        .map(t2 -> liveRanges.find(r -> r.var.nameEquals(t2.a)).get());
                final var b = sameReg.find(r -> r != range && r.end > range.begin && r.begin < range.end);

                if (b.isPresent()) {
                    System.out.println("Live range conflict!!!");
                    System.out.println(range);
                    System.out.println(b.get());
                    return false;
                }

                return true;
            });

            if (check && DEBUG >= 2) {
                System.out.println("Sanity check passed");
            }

            // System.out.println("Function " + fn.functionName.name);
            // System.out.println(fixed.fold("", (acc, n) -> acc + n));
            // liveRanges.map(r -> System.out.printf("%s", r)).reverse();

            return new FunctionInfo(fn.functionName, params, alloc, cfGraph,
                    new Var(fn.block.return_id),
                    new List<>(z.bind(x -> x)));

        })).get();

        final var prgmv = new Program(functionInfos.map(f -> {
            if (DEBUG >= 2) {
                System.out.print(f);
            }
            return f.translate();
        }).toJavaList());

        if (DEBUG >= 2) {
            System.out.println("--------");
        }

        System.out.println(prgmv);
    }
}

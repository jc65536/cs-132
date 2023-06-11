import java.util.function.*;

import cs132.IR.sparrowv.*;
import cs132.IR.sparrowv.visitor.*;

public class TransVisitor implements ArgRetVisitor<SVEnv, Function<List<RVInstr>, List<RVInstr>>> {
    static Function<List<RVInstr>, List<RVInstr>> ident = x -> x;

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Program arg0, SVEnv arg1) {
        return List.fromJavaList(arg0.funDecls)
                .mapi((fun, i) -> fun.accept(this, arg1.setFirstFun(i == 0)))
                .fold(ident, Function::andThen);
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(FunctionDecl arg0, SVEnv arg1) {
        final var ids = arg0.accept(new IdVisitor(), List.nul());
        final var idOffsets = ids.mapi((name, i) -> new T2<>(name, (i + 1) * 4));
        final var env = new SVEnv(arg0.functionName.toString(), 0, idOffsets, arg1.firstFun);

        final var funLabel = new RVLabel(arg0.functionName.toString());

        // Push locals (excluding params) + return addr
        final var frameGrowSize = (ids.count() - arg0.formalParameters.size() + 1) * 4;
        final var frameResetSize = frameGrowSize + arg0.formalParameters.size() * 4;

        return ident
                .andThen(tr -> tr
                        .cons(new Global(funLabel))
                        .cons(funLabel))
                .andThen(arg1.firstFun
                        ? ident
                        : tr -> tr
                                .cons(new RVAddImm(Reg.sp, Reg.sp, -frameGrowSize))
                                .cons(new RVStore(Reg.ra, Reg.sp, 0)))
                .andThen(arg0.block.accept(this, env))
                .andThen(arg1.firstFun
                        ? tr -> tr
                                .cons(new RVLoadImm(Reg.a0, Ecalls.EXIT))
                                .cons(new RVEcall())
                        : tr -> tr
                                .cons(new RVLoad(Reg.ra, Reg.sp, 0))
                                .cons(new RVAddImm(Reg.sp, Reg.sp, frameResetSize))
                                .cons(new RVRet()));
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Block arg0, SVEnv arg1) {
        final var retOffset = arg1.offsetLookup(arg0.return_id.toString());
        return List.fromJavaList(arg0.instructions)
                .fold(new T2<>(ident, arg1), (acc, ins) -> {
                    final var composed = acc.a.andThen(ins.accept(this, acc.b));
                    return new T2<>(composed, acc.b.inc());
                }).a
                .andThen(tr -> tr.cons(new RVLoad(Reg.a0, Reg.sp, retOffset)));
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(LabelInstr arg0, SVEnv arg1) {
        return tr -> tr.cons(arg1.mkLabel(arg0.label.toString()));
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Move_Reg_Integer arg0, SVEnv arg1) {
        return tr -> tr.cons(new RVLoadImm(Reg.from(arg0.lhs), arg0.rhs));
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Move_Reg_FuncName arg0, SVEnv arg1) {
        return tr -> tr.cons(new RVLoadAddr(Reg.from(arg0.lhs), new RVLabel(arg0.rhs.toString())));
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Add arg0, SVEnv arg1) {
        return tr -> tr.cons(new RVAdd(Reg.from(arg0.lhs), Reg.from(arg0.arg1), Reg.from(arg0.arg2)));
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Subtract arg0, SVEnv arg1) {
        return tr -> tr.cons(new RVSub(Reg.from(arg0.lhs), Reg.from(arg0.arg1), Reg.from(arg0.arg2)));
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Multiply arg0, SVEnv arg1) {
        return tr -> tr.cons(new RVMul(Reg.from(arg0.lhs), Reg.from(arg0.arg1), Reg.from(arg0.arg2)));
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(LessThan arg0, SVEnv arg1) {
        return tr -> tr.cons(new RVLessThan(Reg.from(arg0.lhs), Reg.from(arg0.arg1), Reg.from(arg0.arg2)));
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Load arg0, SVEnv arg1) {
        return tr -> tr.cons(new RVLoad(Reg.from(arg0.lhs), Reg.from(arg0.base), arg0.offset));
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Store arg0, SVEnv arg1) {
        return tr -> tr.cons(new RVStore(Reg.from(arg0.rhs), Reg.from(arg0.base), arg0.offset));
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Move_Reg_Reg arg0, SVEnv arg1) {
        return tr -> tr.cons(new RVMove(Reg.from(arg0.lhs), Reg.from(arg0.rhs)));
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Move_Id_Reg arg0, SVEnv arg1) {
        final var offset = arg1.offsetLookup(arg0.lhs.toString());
        return tr -> tr.cons(new RVStore(Reg.from(arg0.rhs), Reg.sp, offset));
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Move_Reg_Id arg0, SVEnv arg1) {
        final var offset = arg1.offsetLookup(arg0.rhs.toString());
        return tr -> tr.cons(new RVLoad(Reg.from(arg0.lhs), Reg.sp, offset));
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Alloc arg0, SVEnv arg1) {
        return tr -> tr
                .cons(new RVMove(Reg.a1, Reg.from(arg0.size)))
                .cons(new RVJumpLink(Subroutine.ALLOC.label))
                .cons(new RVMove(Reg.from(arg0.lhs), Reg.a0));
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Print arg0, SVEnv arg1) {
        return tr -> tr
                .cons(new RVMove(Reg.a1, Reg.from(arg0.content)))
                .cons(new RVJumpLink(Subroutine.PRINT.label));
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(ErrorMessage arg0, SVEnv arg1) {
        if (arg0.msg.equals("null pointer"))
            return tr -> tr
                    .cons(new RVLoadAddr(Reg.a1, ErrMsg.NULL.label))
                    .cons(new RVJump(Subroutine.ERROR.label));
        else
            return tr -> tr
                    .cons(new RVLoadAddr(Reg.a1, ErrMsg.OOB.label))
                    .cons(new RVJump(Subroutine.ERROR.label));
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Goto arg0, SVEnv arg1) {
        return tr -> tr.cons(new RVJump(arg1.mkLabel(arg0.label.toString())));
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(IfGoto arg0, SVEnv arg1) {
        final var actualLabel = arg1.mkLabel(arg0.label.toString());
        final var tempLabel = arg1.mkTempLabel();
        return tr -> tr
                .cons(new RVBranchNonZero(Reg.from(arg0.condition), tempLabel))
                .cons(new RVJump(actualLabel))
                .cons(tempLabel);
    }

    @Override
    public Function<List<RVInstr>, List<RVInstr>> visit(Call arg0, SVEnv arg1) {
        final var argOffsets = List.fromJavaList(arg0.args)
                .mapi((id, i) -> new T2<>(id.toString(), i * 4));

        final var stackGrowSize = argOffsets.count() * 4;

        final var pushArgs = argOffsets.fold(ident, (acc, t) -> {
            final var idOffset = arg1.offsetLookup(t.a) + stackGrowSize;
            return acc.andThen(tr -> tr
                    .cons(new RVLoad(Reg.a1, Reg.sp, idOffset))
                    .cons(new RVStore(Reg.a1, Reg.sp, t.b)));
        });

        return ident
                .andThen(tr -> tr
                        .cons(new RVAddImm(Reg.sp, Reg.sp, -stackGrowSize)))
                .andThen(pushArgs)
                .andThen(tr -> tr
                        .cons(new RVJumpLinkReg(Reg.from(arg0.callee)))
                        .cons(new RVMove(Reg.from(arg0.lhs), Reg.a0)));
    }
}

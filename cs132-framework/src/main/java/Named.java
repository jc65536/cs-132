import cs132.IR.token.*;

interface Code {
    List<RVInstr> toCode(List<RVInstr> tr);
}

public interface Named {
    String name();
}

enum ErrMsg implements Code {
    NULL("err_null", "null pointer"),
    OOB("err_oob", "array index out of bounds");

    final RVLabel label;
    final String msg;

    private ErrMsg(String name, String msg) {
        this.label = new RVLabel(name);
        this.msg = msg;
    }

    @Override
    public List<RVInstr> toCode(List<RVInstr> tr) {
        return tr
                .cons(new Global(label))
                .cons(label)
                .cons(new Ascii(msg));
    }
}

enum Reg implements Named {
    zero, ra, sp, gp, tp, fp,
    a0, a1, a2, a3, a4, a5, a6, a7,
    s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11,
    t0, t1, t2, t3, t4, t5, t6;

    static Reg from(Register r) {
        return Reg.valueOf(r.toString());
    }
}

abstract class Subroutine implements Code {
    final RVLabel label;

    private Subroutine(String name) {
        this.label = new RVLabel(name);
    }

    @Override
    public List<RVInstr> toCode(List<RVInstr> tr) {
        return tr.cons(new Global(label))
                .cons(label);
    }

    static final Subroutine ALLOC = new Subroutine("alloc") {
        @Override
        public List<RVInstr> toCode(List<RVInstr> tr) {
            return super.toCode(tr)
                    .cons(new RVLoadImm(Reg.a0, Ecalls.SBRK))
                    .cons(new RVEcall())
                    .cons(new RVRet());
        }
    };

    static final Subroutine ERROR = new Subroutine("error") {
        final RVLabel loop = new RVLabel("error_loop");

        @Override
        public List<RVInstr> toCode(List<RVInstr> tr) {
            return super.toCode(tr)
                    .cons(new RVLoadImm(Reg.a0, Ecalls.PRINT_STRING))
                    .cons(new RVEcall())
                    .cons(new RVLoadImm(Reg.a1, 10))
                    .cons(new RVLoadImm(Reg.a0, Ecalls.PRINT_CHARACTER))
                    .cons(new RVEcall())
                    .cons(new RVLoadImm(Reg.a0, Ecalls.EXIT))
                    .cons(new RVEcall())
                    .cons(loop)
                    .cons(new RVJump(loop));
        }
    };

    static final Subroutine PRINT = new Subroutine("print") {
        @Override
        public List<RVInstr> toCode(List<RVInstr> tr) {
            return super.toCode(tr)
                    .cons(new RVLoadImm(Reg.a0, Ecalls.PRINT_INT))
                    .cons(new RVEcall())
                    .cons(new RVLoadImm(Reg.a1, 10))
                    .cons(new RVLoadImm(Reg.a0, Ecalls.PRINT_CHARACTER))
                    .cons(new RVEcall())
                    .cons(new RVRet());
        }
    };
}

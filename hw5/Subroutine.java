public abstract class Subroutine implements Usable, Code {
    private final RVLabel label;

    private Subroutine(String name) {
        this.label = new RVLabel(name);
    }

    @Override
    public List<RVInstr> toCode(List<RVInstr> tr) {
        return tr.cons(new Global(label)).cons(label);
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

    @Override
    public String use() {
        return label.use();
    }
}

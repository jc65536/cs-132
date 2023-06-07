import cs132.IR.token.Register;

public class Regs {
    static final Register t0 = new Register("t0");
    static final Register t1 = new Register("t1");

    static final List<Register> argRegs = List.<Register>nul()
            .cons(new Register("a2"))
            .cons(new Register("a3"))
            .cons(new Register("a4"))
            .cons(new Register("a5"))
            .cons(new Register("a6"))
            .cons(new Register("a7"));

    static final List<Register> all = List.<Register>nul()
            .cons(new Register("s1"))
            .cons(new Register("s2"))
            .cons(new Register("s3"))
            .cons(new Register("s4"))
            .cons(new Register("s5"))
            .cons(new Register("s6"))
            .cons(new Register("s7"))
            .cons(new Register("s8"))
            .cons(new Register("s9"))
            .cons(new Register("s10"))
            .cons(new Register("s11"))
            .cons(new Register("t2"))
            .cons(new Register("t3"))
            .cons(new Register("t4"))
            .cons(new Register("t5"));
}

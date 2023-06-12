import cs132.IR.token.*;

public interface Usable {
    String use();
}

class RVLabel extends RVInstr implements Usable {
    private final String name;

    RVLabel(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("%s:", name);
    }

    @Override
    public String use() {
        return name;
    }
}

interface Code {
    List<RVInstr> toCode(List<RVInstr> tr);
}

class Ecalls {
    static final int PRINT_INT = 1;
    static final int PRINT_STRING = 4;
    static final int SBRK = 9;
    static final int EXIT = 10;
    static final int PRINT_CHARACTER = 11;
}

enum ErrMsg implements Usable, Code {
    NULL("err_null", "null pointer"),
    OOB("err_oob", "array index out of bounds");

    private final RVLabel label;
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

    @Override
    public String use() {
        return label.use();
    }
}

enum Reg implements Usable {
    zero, ra, sp, gp, tp, fp,
    a0, a1, a2, a3, a4, a5, a6, a7,
    s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11,
    t0, t1, t2, t3, t4, t5, t6;

    static Reg from(Register r) {
        return Reg.valueOf(r.toString());
    }

    @Override
    public String use() {
        return name();
    }
}

public abstract class RVInstr {
    @Override
    public abstract String toString();
}

class RVLabel extends RVInstr implements Named {
    private final String name;

    RVLabel(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("%s:", name);
    }

    @Override
    public String name() {
        return name;
    }
}

abstract class RVBinop extends RVInstr {
    private final String ins;
    final Reg dest;
    final Reg src1;
    final Reg src2;

    RVBinop(String ins, Reg dest, Reg src1, Reg src2) {
        this.ins = ins;
        this.dest = dest;
        this.src1 = src1;
        this.src2 = src2;
    }

    @Override
    public String toString() {
        return String.format("%s %s, %s, %s", ins, dest.name(), src1.name(), src2.name());
    }
}

class RVAdd extends RVBinop {
    RVAdd(Reg dest, Reg src1, Reg src2) {
        super("add", dest, src1, src2);
    }
}

class RVAddImm extends RVInstr {
    final Reg dest;
    final Reg src;
    final int imm;

    RVAddImm(Reg dest, Reg src, int imm) {
        this.dest = dest;
        this.src = src;
        this.imm = imm;
    }

    @Override
    public String toString() {
        return String.format("addi %s, %s, %d", dest.name(), src.name(), imm);
    }
}

class RVSub extends RVBinop {
    RVSub(Reg dest, Reg src1, Reg src2) {
        super("sub", dest, src1, src2);
    }
}

class RVMul extends RVBinop {
    RVMul(Reg dest, Reg src1, Reg src2) {
        super("mul", dest, src1, src2);
    }
}

class RVLessThan extends RVBinop {
    RVLessThan(Reg dest, Reg src1, Reg src2) {
        super("slt", dest, src1, src2);
    }
}

class RVLoad extends RVInstr {
    final Reg dest;
    final Reg base;
    final int offset;

    RVLoad(Reg dest, Reg base, int offset) {
        this.dest = dest;
        this.base = base;
        this.offset = offset;
    }

    @Override
    public String toString() {
        return String.format("lw %s, %d(%s)", dest.name(), offset, base.name());
    }
}

class RVStore extends RVInstr {
    final Reg src;
    final Reg base;
    final int offset;

    RVStore(Reg src, Reg base, int offset) {
        this.src = src;
        this.base = base;
        this.offset = offset;
    }

    @Override
    public String toString() {
        return String.format("sw %s, %d(%s)", src.name(), offset, base.name());
    }
}

class RVMove extends RVInstr {
    final Reg dest;
    final Reg src;

    RVMove(Reg dest, Reg src) {
        this.dest = dest;
        this.src = src;
    }

    @Override
    public String toString() {
        return String.format("mv %s, %s", dest.name(), src.name());
    }
}

class RVLoadImm extends RVInstr {
    final Reg dest;
    final int imm;

    RVLoadImm(Reg dest, int imm) {
        this.dest = dest;
        this.imm = imm;
    }

    @Override
    public String toString() {
        return String.format("li %s, %d", dest.name(), imm);
    }
}

class RVLoadAddr extends RVInstr {
    final Reg dest;
    final RVLabel label;

    RVLoadAddr(Reg dest, RVLabel label) {
        this.dest = dest;
        this.label = label;
    }

    @Override
    public String toString() {
        return String.format("la %s, %s", dest.name(), label.name());
    }
}

class RVEcall extends RVInstr {
    @Override
    public String toString() {
        return "ecall";
    }
}

class RVJumpLink extends RVInstr {
    final RVLabel label;

    RVJumpLink(RVLabel label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return String.format("jal %s", label.name());
    }
}

class RVJumpLinkReg extends RVInstr {
    final Reg addr;

    RVJumpLinkReg(Reg addr) {
        this.addr = addr;
    }

    @Override
    public String toString() {
        return String.format("jalr %s", addr.name());
    }
}

class RVRet extends RVInstr {
    @Override
    public String toString() {
        return "ret";
    }
}

class RVJump extends RVInstr {
    final RVLabel label;

    RVJump(RVLabel label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return String.format("j %s", label.name());
    }
}

class RVBranchNonZero extends RVInstr {
    final Reg cond;
    final RVLabel label;

    RVBranchNonZero(Reg cond, RVLabel label) {
        this.cond = cond;
        this.label = label;
    }

    @Override
    public String toString() {
        return String.format("bnez %s, %s", cond.name(), label.name());
    }
}

// Directives

class DataSec extends RVInstr {
    @Override
    public String toString() {
        return ".data";
    }
}

class Global extends RVInstr {
    final RVLabel label;

    Global(RVLabel label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return String.format(".globl %s", label.name());
    }
}

class Ascii extends RVInstr {
    final String s;

    Ascii(String s) {
        this.s = s;
    }

    @Override
    public String toString() {
        return String.format(".asciiz \"%s\"", s);
    }
}

public abstract class RVInstr {
    @Override
    public abstract String toString();
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
        return String.format("%s %s, %s, %s", ins, dest.use(), src1.use(), src2.use());
    }
}

class RVAdd extends RVBinop {
    RVAdd(Reg dest, Reg src1, Reg src2) {
        super("add", dest, src1, src2);
    }
}

class RVAdjStack extends RVInstr {
    final int size;

    RVAdjStack(int size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return String.format("addi %s, %s, %d", Reg.sp.use(), Reg.sp.use(), size);
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
        return String.format("lw %s, %d(%s)", dest.use(), offset, base.use());
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
        return String.format("sw %s, %d(%s)", src.use(), offset, base.use());
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
        return String.format("mv %s, %s", dest.use(), src.use());
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
        return String.format("li %s, %d", dest.use(), imm);
    }
}

class RVLoadAddr extends RVInstr {
    final Reg dest;
    final Usable label;

    RVLoadAddr(Reg dest, Usable label) {
        this.dest = dest;
        this.label = label;
    }

    @Override
    public String toString() {
        return String.format("la %s, %s", dest.use(), label.use());
    }
}

class RVEcall extends RVInstr {
    @Override
    public String toString() {
        return "ecall";
    }
}

class RVJumpLink extends RVInstr {
    final Usable label;

    RVJumpLink(Usable label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return String.format("jal %s", label.use());
    }
}

class RVJumpLinkReg extends RVInstr {
    final Reg addr;

    RVJumpLinkReg(Reg addr) {
        this.addr = addr;
    }

    @Override
    public String toString() {
        return String.format("jalr %s", addr.use());
    }
}

class RVRet extends RVInstr {
    @Override
    public String toString() {
        return "ret";
    }
}

class RVJump extends RVInstr {
    final Usable label;

    RVJump(Usable label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return String.format("j %s", label.use());
    }
}

class RVBranchNonZero extends RVInstr {
    final Reg cond;
    final Usable label;

    RVBranchNonZero(Reg cond, Usable label) {
        this.cond = cond;
        this.label = label;
    }

    @Override
    public String toString() {
        return String.format("bnez %s, %s", cond.use(), label.use());
    }
}

// Directives

class Data extends RVInstr {
    @Override
    public String toString() {
        return ".data";
    }
}

class Global extends RVInstr {
    final Usable label;

    Global(Usable label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return String.format(".globl %s", label.use());
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

import cs132.IR.sparrowv.*;
import cs132.IR.token.*;

public class Util {
    static boolean nameEq(Object a, Object b) {
        return a.toString().equals(b.toString());
    }

    static Instruction comment(String s) {
        return new Move_Id_Reg(new Identifier("________" + s), Regs.t0);
    }

    static Instruction comment(cs132.IR.sparrow.Instruction i) {
        return comment(i.toString()
                .replace(' ', '_')
                .replace("=", "_eq_")
                .replace("+", "_p_")
                .replace("-", "_m_")
                .replace("*", "_t_")
                .replace("<", "_lt_")
                .replace("[", "_l_")
                .replace("]", "_r_")
                .replace(":", "")
                .replace("@", "_fn_")
                .replace('(', '_')
                .replace(')', '_')
                .replace('\"', '_'));
    }
}

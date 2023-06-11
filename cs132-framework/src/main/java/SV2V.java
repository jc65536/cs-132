import java.util.Optional;

import cs132.IR.SparrowParser;
import cs132.IR.registers.Registers;
import cs132.IR.syntaxtree.Node;
import cs132.IR.visitor.SparrowVConstructor;

public class SV2V {
    public static void main(String[] args) throws Exception {
        Registers.SetRiscVregs();
        new SparrowParser(System.in);

        final Node root = SparrowParser.Program();

        final SparrowVConstructor ctor = new SparrowVConstructor();

        root.accept(ctor);

        final var prgm = ctor.getProgram();

        final var rvprgm = prgm.accept(new TransVisitor(), new SVEnv("", 0, List.nul()))
                .andThen(Subroutine.ALLOC::toCode)
                .andThen(Subroutine.ERROR::toCode)
                .andThen(Subroutine.PRINT::toCode)
                .andThen(tr -> tr.cons(new DataSec()))
                .andThen(ErrMsg.NULL::toCode)
                .andThen(ErrMsg.OOB::toCode)
                .apply(List.nul());
        
        System.out.println(rvprgm.reverse().strJoin("\n"));
    }
}

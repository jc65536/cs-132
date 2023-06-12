import cs132.IR.*;
import cs132.IR.registers.*;
import cs132.IR.visitor.*;

public class SV2V {
    public static void main(String[] args) throws Exception {
        Registers.SetRiscVregs();
        new SparrowParser(System.in);

        final var root = SparrowParser.Program();
        final var ctor = new SparrowVConstructor();
        root.accept(ctor);
        final var prgm = ctor.getProgram();

        final var rvPrgm = prgm.accept(new TransVisitor(), new SVEnv())
                .andThen(Subroutine.ALLOC::toCode)
                .andThen(Subroutine.ERROR::toCode)
                .andThen(Subroutine.PRINT::toCode)
                .andThen(tr -> tr.cons(new Data()))
                .andThen(ErrMsg.NULL::toCode)
                .andThen(ErrMsg.OOB::toCode)
                .apply(List.nul());

        System.out.println(rvPrgm.reverse().strJoin("\n"));
    }
}

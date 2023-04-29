import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

public class SymPairVisitor extends GJDepthFirst<SymPair, TypeEnv> {
    @Override
    public SymPair visit(VarDeclaration n, TypeEnv argu) {
        final var sym = n.f1.f0.tokenImage;
        final var type = n.f0.accept(new TypeVisitor(), argu);
        return new SymPair(sym, type);
    }

    @Override
    public SymPair visit(FormalParameter n, TypeEnv argu) {
        final var sym = n.f1.f0.tokenImage;
        final var type = n.f0.accept(new TypeVisitor(), argu);
        return new SymPair(sym, type);
    }
}

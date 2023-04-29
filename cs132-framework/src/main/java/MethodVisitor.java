import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

public class MethodVisitor extends GJDepthFirst<Method, TypeEnv> {
    @Override
    public Method visit(MethodDeclaration n, TypeEnv argu) {
        final var name = n.f2.f0.tokenImage;
        final var params = n.f4.accept(new ListVisitor<>(new SymPairVisitor()), argu);
        final var retType = n.f1.accept(new TypeVisitor(), argu);
        return new Method(name, params, retType, n);
    }
}

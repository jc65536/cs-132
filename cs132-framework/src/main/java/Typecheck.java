import cs132.minijava.MiniJavaParser;

public class Typecheck {
    public static void main(String[] args) throws Exception {
        final var root = new MiniJavaParser(System.in).Goal();

        final var typeEnv = root.accept(new TypeDeclVisitor());

        final var typeChecks = typeEnv.classList
                .flatMap(c -> c.get().methods.map(m -> m.body))
                .map(nodes -> nodes.cons(root.f0).forall(node -> node.accept(new TypecheckVisitor(), null)))
                .get();

        if (typeChecks)
            System.out.println("Program type checked successfully");
        else
            Util.error("Typecheck failed");
    }
}

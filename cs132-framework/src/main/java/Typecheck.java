import cs132.minijava.MiniJavaParser;

public class Typecheck {
    public static void main(String[] args) throws Exception {
        final var root = new MiniJavaParser(System.in).Goal();

        final var typeEnv = root.accept(new TypeDeclVisitor());

        // System.out.println(typeEnv);

        final var mainClassTypeChecks = root.f0.accept(new TypecheckVisitor(), typeEnv);

        final var typeChecks = mainClassTypeChecks && typeEnv.classList.forAll(c -> c.get().methods
                .forAll(m -> m.body.accept(new TypecheckVisitor(), typeEnv.enterClass(c).enterMethod(m))));

        if (typeChecks)
            System.out.println("Program type checked successfully");
        else
            Util.error("Typecheck failed");
    }
}

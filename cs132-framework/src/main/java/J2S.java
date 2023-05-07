import java.util.*;

import cs132.minijava.*;

public class J2S {
    static final boolean DEBUG = true;

    static <T> Optional<T> error(String s) {
        System.out.println(DEBUG ? s : "Type error");
        System.exit(1);
        return Optional.empty();
    }

    public static void main(String[] args) throws Exception {
        new MiniJavaParser(System.in);
        final var root = MiniJavaParser.Goal();

        final var mainName = root.f0.f1.f0.tokenImage;

        final var env = new Lazy<TypeEnv>(z -> root.f1
                .accept(new ListVisitor<>(new ClassVisitor()), z)
                .map(c -> Optional.of(c).filter(u -> !c.name.equals(mainName)))
                .foldFalliable(List.<Class>nul(), Named::distinct)
                .or(() -> error("Duplicate classes"))
                .map(classes -> new TypeEnv<>(List.nul(), classes, Optional.empty(), (code, k) -> null))
                .get()).get();

    }
}

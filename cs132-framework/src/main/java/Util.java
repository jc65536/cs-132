import java.util.Optional;

import cs132.minijava.syntaxtree.*;

public class Util {
    static final boolean DEBUG = false;

    static <T> Optional<T> error(String s) {
        if (DEBUG)
            System.out.println(s);
        else
            System.out.println("Type error");
        
        System.exit(1);
        return Optional.empty();
    }

    static boolean checkExpr(Node node, Type expected, TypeEnv argu) {
        return node.accept(new ExprVisitor(), argu)
                .map(type -> type.subtypes(expected))
                .orElse(false);
    }

    static <T extends Named> Optional<T> distinct(List<T> list, T named) {
        return Optional.of(named)
                .filter(u -> !list.exists(n -> n.name().equals(named.name())));
    }
}

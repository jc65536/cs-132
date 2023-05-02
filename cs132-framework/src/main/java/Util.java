import java.util.Optional;

import cs132.minijava.syntaxtree.*;

public class Util {
    static final boolean DEBUG = false;
    static final boolean EXIT = true;

    static <T> Optional<T> error(String s) {
        if (DEBUG)
            System.out.println(s);
        else
            System.out.println("Type error");
        
        if (EXIT)
            System.exit(1);

        return Optional.empty();
    }

    static boolean expect(boolean b, String msg) {
        return b || Util.<Boolean>error(msg).get();
    }

    static boolean checkExpr(Node node, Type expected, TypeEnv argu) {
        return node.accept(new ExprVisitor(), argu)
                .map(type -> type.subtypes(expected))
                .orElse(false);
    }

    static <T> Optional<T> condOpt(T v, boolean b) {
        return Optional.of(v).filter(u -> b);
    }
}

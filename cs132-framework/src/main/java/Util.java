import cs132.minijava.syntaxtree.*;

public class Util {
    static <T> T error(String s) {
        System.out.println("Type error");
        System.exit(1);
        return null;
    }

    static boolean expect(boolean b, String msg) {
        return b || Util.<Boolean>error(msg);
    }

    static boolean checkExpr(Expression node, Type expected, TypeEnv argu) {
        return node.accept(new ExprVisitor(), argu).subtypes(expected);
    }
}

import cs132.minijava.MiniJavaParser;
import cs132.minijava.syntaxtree.Node;
import java.util.*;

public class Typecheck {
    public static void main(String[] args) throws Exception {
        final var root = new MiniJavaParser(System.in).Goal();

        final var k = new MyVisitor().visit(root, null);
    }
}

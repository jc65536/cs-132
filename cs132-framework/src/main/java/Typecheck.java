

import cs132.minijava.MiniJavaParser;
import cs132.minijava.syntaxtree.Node;

import java.util.*;

public class Typecheck {
    public static void main(String[] args) throws Exception {
        final var root = new MiniJavaParser(System.in).Goal();

        final var typeEnv = root.accept(new TypeDeclVisitor());

        final var mainClass = root.f0;
        mainClass.accept(new TypecheckVisitor(), typeEnv);

        // typeEnv.classList.fold(null, (_n, c) -> c.get().methods.foreach(m -> {
        //     m.body.accept(new TypecheckVisitor(), typeEnv);
        // }));
    }
}

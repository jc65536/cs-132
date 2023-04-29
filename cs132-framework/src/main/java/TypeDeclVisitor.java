import java.util.*;

import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

public class TypeDeclVisitor extends GJNoArguDepthFirst<TypeEnv> {
    @Override
    public TypeEnv visit(Goal n) {
        final var mainClassName = n.f0.f1.f0.tokenImage;
        final var typeDeclNodes = n.f1.nodes;

        return new Lazy<TypeEnv>(
                z -> new TypeEnv(List.nul(), typeDeclNodes.stream().reduce(List.nul(), (classes, node) -> {
                    final var clas = node.accept(new ClassVisitor(), z);

                    if (clas.name.equals(mainClassName) || classes.exists(c -> c.name.equals(clas.name)))
                        Util.error("Duplicate class name");

                    return classes.cons(clas);
                }, (u, v) -> v), Optional.empty())).get();
    }
}

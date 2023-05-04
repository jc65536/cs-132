import java.util.*;

import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

public class ExprVisitor extends GJDepthFirst<Optional<? extends Type>, TypeEnv> {
    @Override
    public Optional<? extends Type> visit(Expression n, TypeEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Optional<? extends Type> visit(AndExpression n, TypeEnv argu) {
        return checkBinOp(n.f0, n.f2, Prim.BOOL, Prim.BOOL, argu);
    }

    @Override
    public Optional<? extends Type> visit(CompareExpression n, TypeEnv argu) {
        return checkBinOp(n.f0, n.f2, Prim.INT, Prim.BOOL, argu);
    }

    @Override
    public Optional<? extends Type> visit(PlusExpression n, TypeEnv argu) {
        return checkBinOp(n.f0, n.f2, Prim.INT, Prim.INT, argu);
    }

    @Override
    public Optional<? extends Type> visit(MinusExpression n, TypeEnv argu) {
        return checkBinOp(n.f0, n.f2, Prim.INT, Prim.INT, argu);
    }

    @Override
    public Optional<? extends Type> visit(TimesExpression n, TypeEnv argu) {
        return checkBinOp(n.f0, n.f2, Prim.INT, Prim.INT, argu);
    }

    @Override
    public Optional<? extends Type> visit(ArrayLookup n, TypeEnv argu) {
        return n.f0.accept(this, argu)
                .filter(Prim.ARR::equals)
                .or(() -> Typecheck.error("Indexing non-array value"))
                .<Type>flatMap(u -> n.f2.accept(this, argu))
                .filter(Prim.INT::equals)
                .or(() -> Typecheck.error("Array index not int"));
    }

    @Override
    public Optional<? extends Type> visit(ArrayLength n, TypeEnv argu) {
        return n.f0.accept(this, argu)
                .filter(Prim.ARR::equals)
                .or(() -> Typecheck.error("Array len error"))
                .map(u -> Prim.INT);
    }

    @Override
    public Optional<? extends Type> visit(MessageSend n, TypeEnv argu) {
        final var methodName = n.f2.f0.tokenImage;
        final var argNodes = n.f4;
        return n.f0.accept(this, argu)
                .filter(t -> t instanceof Class)
                .or(() -> Typecheck.error("Method call on primitive"))
                .flatMap(t -> ((Class) t).methodLookup(methodName))
                .filter(m -> argNodes.accept(new ListVisitor<>(this), argu)
                        .mapFalliable(x -> x)
                        .map(m::argsCompat)
                        .orElse(false))
                .map(m -> m.retType);
    }

    @Override
    public Optional<? extends Type> visit(ExpressionRest n, TypeEnv argu) {
        return n.f1.accept(this, argu);
    }

    @Override
    public Optional<? extends Type> visit(PrimaryExpression n, TypeEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Optional<? extends Type> visit(IntegerLiteral n, TypeEnv argu) {
        return Optional.of(Prim.INT);
    }

    @Override
    public Optional<? extends Type> visit(TrueLiteral n, TypeEnv argu) {
        return Optional.of(Prim.BOOL);
    }

    @Override
    public Optional<? extends Type> visit(FalseLiteral n, TypeEnv argu) {
        return Optional.of(Prim.BOOL);
    }

    @Override
    public Optional<? extends Type> visit(Identifier n, TypeEnv argu) {
        return argu.symLookup(n.f0.tokenImage).map(pair -> pair.type);
    }

    @Override
    public Optional<? extends Type> visit(ThisExpression n, TypeEnv argu) {
        return argu.currClass;
    }

    @Override
    public Optional<? extends Type> visit(ArrayAllocationExpression n, TypeEnv argu) {
        return n.f3.accept(this, argu)
                .filter(Prim.INT::equals)
                .or(() -> Typecheck.error("Array alloc error"))
                .map(u -> Prim.ARR);
    }

    @Override
    public Optional<? extends Type> visit(AllocationExpression n, TypeEnv argu) {
        return Optional.of(argu.classLookup(n.f1.f0.tokenImage));
    }

    @Override
    public Optional<? extends Type> visit(NotExpression n, TypeEnv argu) {
        return n.f1.accept(this, argu)
                .filter(Prim.BOOL::equals)
                .or(() -> Typecheck.error("Not expr error"));
    }

    @Override
    public Optional<? extends Type> visit(BracketExpression n, TypeEnv argu) {
        return n.f1.accept(this, argu);
    }

    Optional<? extends Type> checkBinOp(Node lhsNode, Node rhsNode, Type opType, Type exprType, TypeEnv argu) {
        return lhsNode.accept(this, argu)
                .filter(opType::equals)
                .or(() -> Typecheck.error("Lhs error"))
                .flatMap(u -> rhsNode.accept(this, argu))
                .filter(opType::equals)
                .or(() -> Typecheck.error("Rhs error"))
                .map(u -> exprType);
    }
}

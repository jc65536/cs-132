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
                .or(() -> Util.error("Indexing non-array value"))
                .<Type>flatMap(u -> n.f2.accept(this, argu))
                .filter(Prim.INT::equals)
                .or(() -> Util.error("Array index not int"));
    }

    @Override
    public Optional<? extends Type> visit(ArrayLength n, TypeEnv argu) {
        return n.f0.accept(this, argu)
                .filter(Prim.ARR::equals)
                .or(() -> Util.error("Array len error"))
                .map(u -> Prim.INT);
    }

    @Override
    public Optional<? extends Type> visit(MessageSend n, TypeEnv argu) {
        return n.f0.accept(this, argu)
                .filter(objType -> objType instanceof Class)
                .or(() -> Util.error("Method call on primitive"))
                .flatMap(objType -> {
                    final var objClass = (Class) objType;
                    final var methodName = n.f2.f0.tokenImage;

                    return n.f4.accept(new ListVisitor<>(this), argu)
                            .failMap(typeOpt -> typeOpt)
                            .or(() -> Util.error("Arg list error"))
                            .flatMap(argTypes -> objClass.methodLookup(methodName, argTypes))
                            .map(m -> m.retType);
                });
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
                .or(() -> Util.error("Array alloc error"))
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
                .or(() -> Util.error("Not expr error"));
    }

    @Override
    public Optional<? extends Type> visit(BracketExpression n, TypeEnv argu) {
        return n.f1.accept(this, argu);
    }

    Optional<? extends Type> checkBinOp(Node lhsNode, Node rhsNode, Type opType, Type exprType, TypeEnv argu) {
        return lhsNode.accept(this, argu)
                .filter(opType::equals)
                .or(() -> Util.error("Lhs error"))
                .flatMap(u -> rhsNode.accept(this, argu))
                .filter(opType::equals)
                .or(() -> Util.error("Rhs error"))
                .map(u -> exprType);
    }
}

import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

public class ExprVisitor extends GJDepthFirst<Type, TypeEnv> {
    @Override
    public Type visit(Expression n, TypeEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Type visit(AndExpression n, TypeEnv argu) {
        return checkBinOp(n.f0, n.f2, Prim.BOOL, Prim.BOOL, argu);
    }

    @Override
    public Type visit(CompareExpression n, TypeEnv argu) {
        return checkBinOp(n.f0, n.f2, Prim.INT, Prim.BOOL, argu);
    }

    @Override
    public Type visit(PlusExpression n, TypeEnv argu) {
        return checkBinOp(n.f0, n.f2, Prim.INT, Prim.INT, argu);
    }

    @Override
    public Type visit(MinusExpression n, TypeEnv argu) {
        return checkBinOp(n.f0, n.f2, Prim.INT, Prim.INT, argu);
    }

    @Override
    public Type visit(TimesExpression n, TypeEnv argu) {
        return checkBinOp(n.f0, n.f2, Prim.INT, Prim.INT, argu);
    }

    @Override
    public Type visit(ArrayLookup n, TypeEnv argu) {
        if (n.f0.accept(this, argu) == Prim.ARR && n.f2.accept(this, argu) == Prim.INT)
            return Prim.INT;
        else
            return Util.error("Array lookup error");
    }

    @Override
    public Type visit(ArrayLength n, TypeEnv argu) {
        if (n.f0.accept(this, argu) == Prim.ARR)
            return Prim.INT;
        else
            return Util.error("Array length error");
    }

    @Override
    public Type visit(MessageSend n, TypeEnv argu) {
        final var methodName = n.f2.f0.tokenImage;
        final var objType = n.f0.accept(this, argu);

        if (objType instanceof Class) {
            final var objClass = (Class) objType;
            final var argVisitor = new ListVisitor<>(this, (u, v) -> true, "z");
            final var argTypes = n.f4.accept(argVisitor, argu);

            return objClass.methodLookup(methodName, argTypes).retType;
        } else {
            return Util.error("Method call on primitive");
        }
    }

    @Override
    public Type visit(ExpressionRest n, TypeEnv argu) {
        return n.f1.accept(this, argu);
    }

    @Override
    public Type visit(PrimaryExpression n, TypeEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Type visit(IntegerLiteral n, TypeEnv argu) {
        return Prim.INT;
    }

    @Override
    public Type visit(TrueLiteral n, TypeEnv argu) {
        return Prim.BOOL;
    }

    @Override
    public Type visit(FalseLiteral n, TypeEnv argu) {
        return Prim.BOOL;
    }

    @Override
    public Type visit(Identifier n, TypeEnv argu) {
        return argu.symLookup(n.f0.tokenImage).type;
    }

    @Override
    public Type visit(ThisExpression n, TypeEnv argu) {
        return argu.currClass.orElseGet(() -> Util.error("this error"));
    }

    @Override
    public Type visit(ArrayAllocationExpression n, TypeEnv argu) {
        if (n.f3.accept(this, argu) == Prim.INT)
            return Prim.ARR;
        else
            return Util.error("Array allocation error");
    }

    @Override
    public Type visit(AllocationExpression n, TypeEnv argu) {
        return argu.classLookup(n.f1.f0.tokenImage);
    }

    @Override
    public Type visit(NotExpression n, TypeEnv argu) {
        if (n.f1.accept(this, argu) == Prim.BOOL)
            return Prim.BOOL;
        else
            return Util.error("Not expression error");
    }

    @Override
    public Type visit(BracketExpression n, TypeEnv argu) {
        return n.f1.accept(this, argu);
    }

    Type checkBinOp(Node lhsNode, Node rhsNode, Type opType, Type exprType, TypeEnv argu) {
        if (lhsNode.accept(this, argu) == opType && rhsNode.accept(this, argu) == opType)
            return exprType;
        else
            return Util.error("Binop error");
    }
}

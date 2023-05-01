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
        final var arrType = n.f0.accept(this, argu);
        final var idxType = n.f2.accept(this, argu);

        if (arrType == Prim.ARR && idxType == Prim.INT)
            return Prim.INT;
        else
            return Util.error("Array lookup error");
    }

    @Override
    public Type visit(ArrayLength n, TypeEnv argu) {
        final var arrType = n.f0.accept(this, argu);

        if (arrType == Prim.ARR)
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
        final var exprNode = n.f0.choice;
        return exprNode.accept(this, argu);
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
        final var sym = n.f0.tokenImage;
        return argu.symLookup(sym).type;
    }

    @Override
    public Type visit(ThisExpression n, TypeEnv argu) {
        return argu.currClass.orElseGet(() -> Util.error("this error"));
    }

    @Override
    public Type visit(ArrayAllocationExpression n, TypeEnv argu) {
        final var lenExpr = n.f3;

        if (lenExpr.accept(this, argu) == Prim.INT)
            return Prim.ARR;
        else
            return Util.error("Array allocation error");
    }

    @Override
    public Type visit(AllocationExpression n, TypeEnv argu) {
        final var className = n.f1.f0.tokenImage;
        return argu.classLookup(className);
    }

    @Override
    public Type visit(NotExpression n, TypeEnv argu) {
        final var expr = n.f1;

        if (expr.accept(this, argu) == Prim.BOOL)
            return Prim.BOOL;
        else
            return Util.error("Not expression error");
    }

    @Override
    public Type visit(BracketExpression n, TypeEnv argu) {
        return n.f1.accept(this, argu);
    }

    Type checkBinOp(Node lhsNode, Node rhsNode, Type opType, Type exprType, TypeEnv argu) {
        final var lhsType = lhsNode.accept(this, argu);
        final var rhsType = rhsNode.accept(this, argu);

        if (lhsType == opType && rhsType == opType)
            return exprType;
        else
            return Util.error("Binop error");
    }
}

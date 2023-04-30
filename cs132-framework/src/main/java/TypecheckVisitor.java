import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.*;

class ExprVisitor extends GJDepthFirst<Type, TypeEnv> {
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

public class TypecheckVisitor extends GJDepthFirst<Boolean, TypeEnv> {
    @Override
    public Boolean visit(MainClass n, TypeEnv argu) {
        final var argsSym = n.f11.f0.tokenImage;

        final var localVisitor = new ListVisitor<>(new SymPairVisitor(),
                (symList, pair) -> !(pair.sym.equals(argsSym) || symList.exists(s -> s.sym.equals(pair.sym))),
                "Duplicate locals");

        final var locals = n.f14.accept(localVisitor, argu);

        final var typeEnv = argu.addLocals(locals);
        final var stmtNodes = n.f15.nodes;

        return stmtNodes.stream().allMatch(node -> node.accept(this, typeEnv));
    }

    @Override
    public Boolean visit(MethodDeclaration n, TypeEnv argu) {
        final var localVisitor = new ListVisitor<>(new SymPairVisitor(),
                (symList, pair) -> !symList.exists(s -> s.sym.equals(pair.sym)),
                "Duplicate locals");

        final var locals = n.f7.accept(localVisitor, argu);

        final TypeEnv typeEnv = argu.addLocals(locals);
        final var stmtNodes = n.f8.nodes;
        final var retType = typeEnv.currMethod.get().retType;

        return Util.expect(exprSubtypes(n.f10, retType, typeEnv), "Return type error")
                && stmtNodes.stream().allMatch(node -> node.accept(this, typeEnv));
    }

    @Override
    public Boolean visit(Statement n, TypeEnv argu) {
        return n.f0.choice.accept(this, argu);
    }

    @Override
    public Boolean visit(Block n, TypeEnv argu) {
        return n.f1.nodes.stream().allMatch(node -> node.accept(this, argu));
    }

    @Override
    public Boolean visit(AssignmentStatement n, TypeEnv argu) {
        final var destName = n.f0.f0.tokenImage;
        final var destType = argu.symLookup(destName).type;

        return Util.expect(exprSubtypes(n.f2, destType, argu), "Assignment error");
    }

    @Override
    public Boolean visit(ArrayAssignmentStatement n, TypeEnv argu) {
        final var arrName = n.f0.f0.tokenImage;

        return Util.expect(argu.symLookup(arrName).type == Prim.ARR
                && exprSubtypes(n.f2, Prim.INT, argu)
                && exprSubtypes(n.f5, Prim.INT, argu),
                "Array assignment error");
    }

    @Override
    public Boolean visit(IfStatement n, TypeEnv argu) {
        return Util.expect(exprSubtypes(n.f2, Prim.BOOL, argu)
                && n.f4.accept(this, argu)
                && n.f6.accept(this, argu),
                "If error");
    }

    @Override
    public Boolean visit(WhileStatement n, TypeEnv argu) {
        return Util.expect(exprSubtypes(n.f2, Prim.BOOL, argu)
                && n.f4.accept(this, argu),
                "While error");
    }

    @Override
    public Boolean visit(PrintStatement n, TypeEnv argu) {
        return Util.expect(exprSubtypes(n.f2, Prim.INT, argu), "Print error");
    }

    boolean exprSubtypes(Expression node, Type expected, TypeEnv argu) {
        return node.accept(new ExprVisitor(), argu).subtypes(expected);
    }
}

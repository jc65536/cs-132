import cs132.minijava.visitor.GJNoArguDepthFirst;
import cs132.minijava.syntaxtree.*;
/***
 * A Simple counter counting the number of statements in a minijava program.
 * */

// We implement it in a *functional* way by recursively traverse the syntax tree
public class InstrCounter extends GJNoArguDepthFirst<Integer> {
    @Override
    public Integer visit(Statement n){
        NodeChoice nodeChoice = n.f0;
        return nodeChoice.accept(this);
    }

    @Override
    public Integer visit(Block n) {
        int counter = 0;
        NodeListOptional statements = n.f1;
        for (Node node : statements.nodes){
            counter += node.accept(this);
        }
        return counter;
    }

    @Override
    public Integer visit(AssignmentStatement n) {
        return 1;
    }

    @Override
    public Integer visit(ArrayAssignmentStatement n) {
        return 1;
    }

    @Override
    public Integer visit(PrintStatement n) {
        return 1;
    }

    @Override
    public Integer visit(IfStatement n) {
        int counter = 0;
        Statement true_branch = n.f4;
        counter += true_branch.accept(this);
        Statement false_branch = n.f6;
        counter += false_branch.accept(this);
        return counter + 1;
    }

    @Override
    public Integer visit(WhileStatement n) {
        return 1 + n.f4.accept(this);
    }

    @Override
    public Integer visit(Goal n) {
        int counter = 0;
        MainClass main = n.f0;
        NodeListOptional others = n.f1;

        NodeListOptional stmts_main = main.f15;
        for (Node s : stmts_main.nodes) {
            counter += s.accept(this);
        }

        for (Node cls : others.nodes) {
            counter += cls.accept(this);
        }

        return counter;
    }

    @Override
    public Integer visit(TypeDeclaration n){
        return n.f0.accept(this);
    }

    @Override
    public Integer visit(ClassDeclaration n) {
        int counter = 0;
        NodeListOptional methods = n.f4;
        for (Node m : methods.nodes) {
            counter += m.accept(this);
        }
        return counter;
    }

    @Override
    public Integer visit(MethodDeclaration n) {
        int counter = 0;
        NodeListOptional stmt_list = n.f8;
        for (Node stmt : stmt_list.nodes) {
            counter += stmt.accept(this);
        }
        return counter;
    }

}

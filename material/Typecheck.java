import java.io.InputStream;

import cs132.minijava.syntaxtree.*;
import cs132.minijava.MiniJavaParser;
import cs132.minijava.ParseException;

public class Typecheck {
    public static void main(String [] args) {
        try{
            InputStream in = System.in;
            Node root = new MiniJavaParser(in).Goal();
            InstrCounter counter = new InstrCounter();
            int stmt_num = root.accept(counter);
            System.out.println("Number of statements: " + stmt_num);

        } catch (ParseException e) {
            System.exit(1);
        }
    }
}

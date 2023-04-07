import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

interface Matchable {
    public Optional<List<Token>> match(List<Token> tokens);
}

enum Token implements Matchable {
    LBRACE("{"), RBRACE("}"),
    LPAREN("("), RPAREN(")"),
    LBRACK("["), RBRACK("]"),
    INT("int"), BOOL("boolean"), STRING("String"),
    CLASS("class"), EXTENDS("extends"), PUBLIC("public"),
    STATIC("static"), VOID("void"), MAIN("main"),
    IF("if"), ELSE("else"), WHILE("while"), RETURN("return"),
    LENGTH("length"), THIS("this"), NEW("new"),
    TRUE("true"), FALSE("false"), PRINT("System.out.println"),
    AND("&&"), LT("<"), PLUS("+"), MINUS("-"), MULT("*"),
    SEMCLN(";"), EXCLAM("!"), COMMA(","), DOT("."), ASSIGN("="),

    INT_LIT((c, i) -> Character.isDigit(c)),

    ID_LIT((c, i) -> Character.isLetter(c) || c == '_' || (i > 0 && Character.isDigit(c))),

    WHTSPC((c, i) -> Character.isWhitespace(c)),

    EOF("");

    private final boolean finite;
    private final BiFunction<Character, Integer, Boolean> matchChar;
    public String str;

    private Token(String str) {
        this.finite = true;
        this.str = str;
        this.matchChar = null;
    }

    private Token(BiFunction<Character, Integer, Boolean> matchChar) {
        this.finite = false;
        this.str = "";
        this.matchChar = matchChar;
    }

    public boolean isCharAt(char c, int i) {
        if (finite)
            return i < str.length() && str.charAt(i) == c;

        final var ret = matchChar.apply(c, i);

        if (ret)
            str += c;

        return ret;
    }

    public boolean isEntirelyMatched(int n) {
        return finite ? n == str.length() : n > 0;
    }

    public Optional<List<Token>> match(List<Token> tokens) {
        if (!tokens.isEmpty() && tokens.get(0) == this)
            return Optional.of(tokens.subList(1, tokens.size()));
        else
            return Optional.empty();
    }
}

public class Tokenizer {
    static final BufferedInputStream in = new BufferedInputStream(System.in);

    static void consume() {
        try {
            in.read();
        } catch (Exception e) {
            e.printStackTrace();
            error("IO error");
        }
    }

    static int peek() {
        try {
            in.mark(1);
            final var c = in.read();
            in.reset();
            return c;
        } catch (Exception e) {
            e.printStackTrace();
            error("IO error");
        }
        return -2;
    }

    static void error(String s) {
        System.out.println("Parse error: " + s);
        System.exit(1);
    }

    static Optional<Token> findMatches(int n, List<Token> p) {
        return p.stream()
                .filter(t -> t.isEntirelyMatched(n))
                .findAny();
    }

    static Optional<Token> nextToken(int numMatched, List<Token> possible) {
        final var nextChar = peek();

        if (nextChar == -1)
            return findMatches(numMatched, possible);

        final var nextPossible = possible.stream()
                .filter(t -> t.isCharAt((char) nextChar, numMatched))
                .collect(Collectors.toList());

        if (nextPossible.isEmpty())
            return findMatches(numMatched, possible);

        consume();

        return nextToken(numMatched + 1, nextPossible);
    }

    static List<Token> tokenize(List<Token> tokens) {
        final var opt = nextToken(0, List.of(Token.values()));

        if (opt.isEmpty())
            error("invalid token");

        final var t = opt.get();

        if (t == Token.EOF)
            return tokens;
        else if (t != Token.WHTSPC)
            tokens.add(t);

        return tokenize(tokens);
    }

    public static void main(String[] args) {
        final var tokens = tokenize(new ArrayList<>());

        System.out.println(tokens);
    }
}

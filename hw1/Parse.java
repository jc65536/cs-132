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
    IF("if"), ELSE("else"), WHILE("while"),
    TRUE("true"), FALSE("false"), PRINT("System.out.println"),
    SEMCLN(";"), EXCLAM("!"),

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

enum NonTerm implements Matchable {
    S, L, E;

    private List<List<Matchable>> rules;

    public void setRules(List<List<Matchable>> rules) {
        this.rules = rules;
    }

    private Optional<List<Token>> matchRule(Optional<List<Token>> prev,
            Iterator<Matchable> rule) {

        if (!rule.hasNext() || prev.isEmpty())
            return prev;

        return matchRule(rule.next().match(prev.get()), rule);
    }

    public Optional<List<Token>> match(List<Token> tokens) {
        return this.rules.stream()
                .map(rule -> matchRule(Optional.of(tokens), rule.iterator()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }
}

public class Parse {
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
        System.out.println("Parse error");
        // System.out.println("Parse error: " + s);
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
        NonTerm.S.setRules(List.of(
                List.of(Token.LBRACE, NonTerm.L, Token.RBRACE),
                List.of(Token.PRINT, Token.LPAREN, NonTerm.E, Token.RPAREN, Token.SEMCLN),
                List.of(Token.IF, Token.LPAREN, NonTerm.E, Token.RPAREN, NonTerm.S, Token.ELSE, NonTerm.S),
                List.of(Token.WHILE, Token.LPAREN, NonTerm.E, Token.RPAREN, NonTerm.S)));

        NonTerm.L.setRules(List.of(
                List.of(NonTerm.S, NonTerm.L),
                List.of()));

        NonTerm.E.setRules(List.of(
                List.of(Token.TRUE),
                List.of(Token.FALSE),
                List.of(Token.EXCLAM, NonTerm.E)));

        final var tokens = tokenize(new ArrayList<>());

        // System.out.println(tokens);

        final var res = NonTerm.S.match(tokens);

        if (res.isPresent() && res.get().isEmpty())
            System.out.println("Program parsed successfully");
        else
            error("couldn't match S or remainder exists");
    }
}

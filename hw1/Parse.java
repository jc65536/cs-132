import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

interface Matchable {
    public Optional<List<Token>> match(List<Token> tokens);
}

enum Token implements Matchable {
    LBRACE("{"), RBRACE("}"),
    LPAREN("("), RPAREN(")"),
    IF("if"), ELSE("else"),
    TRUE("true"), FALSE("false"),
    WHILE("while"), PRINT("System.out.println"),
    SEMCLN(";"), EXCLAM("!"),
    EOF(null);

    public static final List<Token> all = List.of(LBRACE, RBRACE, LPAREN,
            RPAREN, IF, ELSE, TRUE, FALSE, WHILE, PRINT, SEMCLN, EXCLAM);

    public final String str;

    private Token(String str) {
        this.str = str;
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

    static boolean isSep(int c) {
        return c == -1 || !Character.isLetter((char) c);
    }

    static void error(String s) {
        System.out.println("Parse error");
        // System.out.println("Parse error: " + s);
        System.exit(1);
    }

    static Token matchToken(int numMatched, List<Token> possible) {
        if (possible.isEmpty())
            error("invalid token");

        final var nextChar = peek();

        final var match = possible.stream()
                .filter(t -> t.str.length() == numMatched
                        && (isSep(t.str.charAt(numMatched - 1))
                                || isSep(nextChar)))
                .findAny();

        if (match.isPresent())
            return match.get();

        consume();

        if (numMatched == 0) {
            if (nextChar == -1)
                return Token.EOF;
            else if (Character.isWhitespace(nextChar))
                return matchToken(0, possible);
        }

        return matchToken(numMatched + 1, possible.stream()
                .filter(t -> t.str.length() > numMatched
                        && t.str.charAt(numMatched) == nextChar)
                .collect(Collectors.toList()));
    }

    static List<Token> tokenize(List<Token> tokens) {
        final var t = matchToken(0, Token.all);

        if (t == Token.EOF) {
            return tokens;
        } else {
            tokens.add(t);
            return tokenize(tokens);
        }
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

        NonTerm.E.setRules(List.of(List.of(Token.TRUE),
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

import java.util.*;
import java.util.function.*;

// Utility
// I originally used T2 (and even T3) to return tuples, but then continuation
// passing style lessened the need to actually store the return value in
// variables. Instead of returning multiple values, I pass in a callback
// function that can receive multiple arguments.

class T2<A, B> {
    final A a;
    final B b;

    T2(A a, B b) {
        this.a = a;
        this.b = b;
    }

    <T> T then(Function<? super A, Function<? super B, T>> f) {
        return f.apply(a).apply(b);
    }
}

// Lazy serves 3 purposes: 1) delay computation 2) cache computed values, and
// 3) eliminate the need for mutable values elsewhere.

// Tbh whenever I ran into stack overflow errors due to infinite mutual
// recursion, making one side Lazy usually fixed the issue. I haven't fully
// thought about the data dependencies between parts of my code.

class Lazy<T> implements Supplier<T> {
    Supplier<T> s;

    Lazy(Supplier<T> s) {
        this.s = s;
    }

    // Hack for self referential lazy values    
    Lazy(Function<Lazy<T>, T> f) {
        this.s = () -> f.apply(this);
    }

    @Override
    public T get() {
        final var v = s.get();
        s = () -> v;
        return v;
    }

    <U> Lazy<U> then(Function<T, U> f) {
        return new Lazy<>(() -> f.apply(get()));
    }

    <U> Lazy<U> bind(Function<T, Supplier<U>> f) {
        return new Lazy<>(() -> f.apply(get()).get());
    }
}

interface ListInt<T> {
    Optional<T> find(Predicate<? super T> p);

    List<T> filter(Predicate<? super T> p);

    <U> U fold(U acc, BiFunction<U, T, U> f);

    <U> List<U> map(Function<T, U> f);

    <U> List<U> flatMap(Function<T, List<U>> f);

    List<T> join(List<? extends T> other);

    int count();

    Optional<Integer> firstIndex(int i, Predicate<? super T> p);

    List<T> unique(List<T> hist, BiPredicate<T, T> eq);
}

// Lazy linked list implementation

public class List<T> extends Lazy<Optional<Pair<T>>> implements ListInt<T> {
    List(Supplier<Optional<Pair<T>>> s) {
        super(s);
    }

    @Override
    public Optional<T> find(Predicate<? super T> p) {
        return get().flatMap(n -> n.find(p));
    }

    static <T> List<T> nul() {
        return new List<>(Optional::empty);
    }

    List<T> cons(T val) {
        return new List<>(() -> Optional.of(new Pair<>(val, this)));
    }

    @Override
    public <U> List<U> map(Function<T, U> f) {
        return new List<>(bind(opt -> opt.map(n -> n.map(f)).orElse(List.nul())));
    }

    @Override
    public <U> U fold(U acc, BiFunction<U, T, U> f) {
        return get().map(n -> n.fold(acc, f)).orElse(acc);
    }

    java.util.List<T> toJavaList() {
        return fold(new ArrayList<>(), (l, v) -> {
            l.add(v);
            return l;
        });
    }

    @Override
    public <U> List<U> flatMap(Function<T, List<U>> f) {
        return new List<>(bind(opt -> opt.map(n -> n.flatMap(f)).orElse(List.nul())));
    }

    @Override
    public List<T> join(List<? extends T> other) {
        return new List<>(bind(opt -> opt.map(n -> n.join(other)).orElse(other.map(x -> x))));
    }

    @Override
    public List<T> filter(Predicate<? super T> p) {
        return new List<>(bind(opt -> opt.map(n -> n.filter(p)).orElse(List.nul())));
    }

    private final Lazy<Integer> count = then(opt -> opt.map(Pair::count).orElse(0));

    @Override
    public int count() {
        return count.get();
    }

    List<T> reverse() {
        return fold(List.nul(), List::cons);
    }

    @Override
    public Optional<Integer> firstIndex(int i, Predicate<? super T> p) {
        return get().flatMap(n -> n.firstIndex(i, p));
    }

    Optional<Integer> firstIndex(Predicate<? super T> p) {
        return firstIndex(0, p);
    }

    Optional<T> head() {
        return get().map(n -> n.val);
    }

    @Override
    public List<T> unique(List<T> hist, BiPredicate<T, T> eq) {
        return new List<>(bind(opt -> opt.map(n -> n.unique(hist, eq)).orElse(List.nul())));
    }

    List<T> unique(BiPredicate<T, T> eq) {
        return unique(List.nul(), eq);
    }
}

class Pair<T> implements ListInt<T> {
    final T val;
    final List<T> next;

    Pair(T val, Supplier<Optional<Pair<T>>> next) {
        this.val = val;
        this.next = new List<>(next);
    }

    @Override
    public Optional<T> find(Predicate<? super T> p) {
        return Optional.of(val).filter(p).or(() -> next.find(p));
    }

    @Override
    public <U> List<U> map(Function<T, U> f) {
        return next.map(f).cons(f.apply(val));
    }

    @Override
    public <U> U fold(U acc, BiFunction<U, T, U> f) {
        return next.fold(f.apply(acc, val), f);
    }

    @Override
    public <U> List<U> flatMap(Function<T, List<U>> f) {
        return f.apply(val).join(next.flatMap(f));
    }

    @Override
    public List<T> join(List<? extends T> other) {
        return next.join(other).cons(val);
    }

    @Override
    public List<T> filter(Predicate<? super T> p) {
        if (p.test(val))
            return next.filter(p).cons(val);
        else
            return next.filter(p);
    }

    @Override
    public int count() {
        return 1 + next.count();
    }

    @Override
    public Optional<Integer> firstIndex(int i, Predicate<? super T> p) {
        return Optional.of(i).filter(u -> p.test(val)).or(() -> next.firstIndex(i + 1, p));
    }

    @Override
    public List<T> unique(List<T> hist, BiPredicate<T, T> eq) {
        return hist.find(v -> eq.test(v, val))
                .map(u -> next.unique(hist, eq))
                .orElseGet(() -> next.unique(hist.cons(val), eq).cons(val));
    }
}

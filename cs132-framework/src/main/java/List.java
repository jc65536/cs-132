import java.util.*;
import java.util.function.*;

class T2<A, B> {
    final A a;
    final B b;

    T2(A a, B b) {
        this.a = a;
        this.b = b;
    }
}

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

// Lazy linked list implementation

class Pair<T> {
    final T val;
    final List<T> next;

    Pair(T val, Supplier<Optional<Pair<T>>> next) {
        this.val = val;
        this.next = new List<>(next);
    }
}

public class List<T> extends Lazy<Optional<Pair<T>>> {
    List(Supplier<Optional<Pair<T>>> s) {
        super(s);
    }

    static <T> List<T> nul() {
        return new List<>(Optional::empty);
    }

    List<T> cons(T val) {
        return new List<>(() -> Optional.of(new Pair<>(val, this)));
    }

    static <T> List<T> fromJavaList(java.util.List<T> l) {
        return fromIterator(l.iterator());
    }

    private static <T> List<T> fromIterator(Iterator<T> it) {
        return new List<T>(() -> Optional.of(0)
                .filter(u -> it.hasNext())
                .map(u -> new Pair<T>(it.next(), fromIterator(it))));
    }

    Optional<T> find(Predicate<? super T> p) {
        return get().flatMap(n -> Optional.of(n.val).filter(p)
                .or(() -> n.next.find(p)));
    }

    <U> List<U> map(Function<T, U> f) {
        return new List<>(bind(opt -> opt
                .map(n -> n.next.map(f).cons(f.apply(n.val)))
                .orElse(List.nul())));
    }

    <U> U fold(U acc, BiFunction<U, T, U> f) {
        return get().map(n -> n.next.fold(f.apply(acc, n.val), f))
                .orElse(acc);
    }

    List<T> join(List<? extends T> other) {
        return new List<>(bind(opt -> opt
                .map(n -> n.next.join(other).cons(n.val))
                .orElse(other.map(x -> x))));
    }

    private final Lazy<Integer> count = then(opt -> opt
            .map(n -> n.next.count() + 1).orElse(0));

    int count() {
        return count.get();
    }

    List<T> reverse() {
        return fold(List.nul(), List::cons);
    }

    private List<T> unique(List<T> hist, BiPredicate<? super T, ? super T> eq) {
        return new List<>(bind(opt -> opt
                .map(n -> hist
                        .find(v -> eq.test(v, n.val))
                        .map(u -> n.next.unique(hist, eq))
                        .orElseGet(() -> n.next.unique(hist.cons(n.val), eq)
                                .cons(n.val)))
                .orElse(List.nul())));
    }

    List<T> unique(BiPredicate<? super T, ? super T> eq) {
        return unique(List.nul(), eq);
    }

    private <U> List<U> mapi(BiFunction<T, Integer, U> f, int i) {
        return new List<>(bind(opt -> opt
                .map(n -> n.next.mapi(f, i + 1).cons(f.apply(n.val, i)))
                .orElse(List.nul())));
    }

    <U> List<U> mapi(BiFunction<T, Integer, U> f) {
        return mapi(f, 0);
    }

    String strJoin(String delim) {
        return get().map(n -> n.next.fold(n.val.toString(),
                (acc, v) -> acc + delim + v))
                .orElse("");
    }
}

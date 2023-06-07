import java.util.*;
import java.util.function.*;

@FunctionalInterface
interface F3<A, B, C, R> {
    R apply(A a, B b, C c);
}

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

    static <A, B> T2<List<A>, List<B>> unwrap(Lazy<T2<List<A>, List<B>>> z) {
        return new T2<>(new List<>(z.bind(t -> t.a)), new List<>(z.bind(t -> t.b)));
    }
}

class T3<A, B, C> extends T2<A, B> {
    final C c;

    T3(A a, B b, C c) {
        super(a, b);
        this.c = c;
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

    static <T> List<T> of(T val) {
        return List.<T>nul().cons(val);
    }

    Optional<T> head() {
        return get().map(n -> n.val);
    }

    <U> U decons(BiFunction<Optional<T>, List<T>, U> k) {
        return get().map(n -> k.apply(Optional.of(n.val), n.next))
                .orElseGet(() -> k.apply(Optional.empty(), List.nul()));
    }

    java.util.List<T> toJavaList() {
        return fold(new ArrayList<>(), (l, v) -> {
            l.add(v);
            return l;
        });
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

    boolean exists(Predicate<? super T> p) {
        return find(p).isPresent();
    }

    boolean forAll(Predicate<? super T> p) {
        return !exists(p.negate());
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

    <U> List<U> flatMap(Function<T, List<U>> f) {
        return new List<>(bind(opt -> opt
                .map(n -> f.apply(n.val).join(n.next.flatMap(f)))
                .orElse(List.nul())));
    }

    List<T> filter(Predicate<? super T> p) {
        return new List<>(bind(opt -> opt
                .map(n -> {
                    final var nextFilter = n.next.filter(p);
                    return Optional.of(n.val)
                            .filter(p)
                            .map(nextFilter::cons)
                            .orElse(nextFilter);
                })
                .orElse(List.nul())));
    }

    private final Lazy<Integer> count = then(opt -> opt
            .map(n -> n.next.count() + 1).orElse(0));

    int count() {
        return count.get();
    }

    List<T> reverse() {
        return fold(List.nul(), List::cons);
    }

    Optional<T> max(BiFunction<T, T, Double> cmp) {
        return fold(Optional.empty(), (acc, val) -> acc
                .filter(m -> cmp.apply(m, val) > 0)
                .or(() -> Optional.of(val)));
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

    List<T2<T, Integer>> enumerate() {
        return mapi(T2::new);
    }

    T2<List<T>, List<T>> partition(Predicate<? super T> p) {
        return fold(new T2<>(List.nul(), List.nul()),
                (acc, v) -> p.test(v)
                        ? new T2<>(acc.a.cons(v), acc.b)
                        : new T2<>(acc.a, acc.b.cons(v)));
    }

    private static <U, V> T3<List<T2<U, V>>, List<U>, List<V>> zip(T3<List<T2<U, V>>, List<U>, List<V>> acc, List<U> l1,
            List<V> l2) {
        return l1.get()
                .map(n1 -> l2.get()
                        .map(n2 -> zip(new T3<>(acc.a.cons(new T2<>(n1.val, n2.val)), acc.b, acc.c), n1.next, n2.next))
                        .orElseGet(() -> new T3<>(acc.a, l1, acc.c)))
                .orElseGet(() -> l2.get().map(u -> new T3<>(acc.a, acc.b, l2))
                        .orElse(acc));
    }

    static <U, V> T3<List<T2<U, V>>, List<U>, List<V>> zip(List<U> l1, List<V> l2) {
        return zip(new T3<>(List.nul(), List.nul(), List.nul()), l1, l2);
    }

    private T2<List<T>, List<T>> split(T2<List<T>, List<T>> acc, boolean flag) {
        return get().map(n -> n.next.split(flag
                ? new T2<>(acc.a.cons(n.val), acc.b)
                : new T2<>(acc.a, acc.b.cons(n.val)),
                !flag)).orElse(acc);
    }

    private List<T> merge(List<T> l1, List<T> l2, BiFunction<T, T, Integer> cmp) {
        return l1.get()
                .map(n1 -> l2.get()
                        .map(n2 -> cmp.apply(n1.val, n2.val) < 0
                                ? merge(n1.next, l2, cmp).cons(n1.val)
                                : merge(l1, n2.next, cmp).cons(n2.val))
                        .orElseGet(() -> l1))
                .orElseGet(() -> l2.get()
                        .map(u -> l2)
                        .orElse(List.nul()));
    }

    List<T> sort(BiFunction<T, T, Integer> cmp) {
        if (count() <= 1) {
            return this;
        } else {
            final var frags = this.split(new T2<>(List.nul(), List.nul()), true);
            return merge(frags.a.sort(cmp), frags.b.sort(cmp), cmp);
        }
    }

    private <U> U foldI(U acc, F3<U, T, Integer, U> f, int i) {
        return get().map(p -> p.next.foldI(f.apply(acc, p.val, i), f, i + 1))
                .orElse(acc);
    }

    <U> U foldI(U acc, F3<U, T, Integer, U> f) {
        return foldI(acc, f, 0);
    }

    String strJoin(String delim) {
        return get().map(n -> n.next.fold(n.val.toString(),
                (acc, v) -> acc + delim + v))
                .orElse("");
    }
}

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

interface ListInt<T> {
    Optional<T> find(Predicate<? super T> p);

    List<T> filter(Predicate<? super T> p);

    <U> U fold(U acc, BiFunction<U, T, U> f);

    <U> List<U> map(Function<T, U> f);

    <U> List<U> flatMap(Function<T, List<U>> f);

    List<T> join(List<? extends T> other);

    int count();

    Optional<Integer> firstIndex(int i, Predicate<? super T> p);

    List<T> unique(List<T> hist, BiPredicate<? super T, ? super T> eq);
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

    boolean exists(Predicate<? super T> p) {
        return find(p).isPresent();
    }

    boolean forAll(Predicate<? super T> p) {
        return !exists(p.negate());
    }

    static <T> List<T> nul() {
        return new List<>(Optional::empty);
    }

    List<T> cons(T val) {
        return new List<>(() -> Optional.of(new Pair<>(val, this)));
    }

    List<T> consOpt(Optional<? extends T> opt) {
        return opt.map(this::cons).orElse(this);
    }

    static <T> List<T> of(T val) {
        return List.<T>nul().cons(val);
    }

    static <T> List<T> ofOpt(Optional<T> opt) {
        return opt.map(List::of).orElse(List.nul());
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

    static <T> List<T> fromJavaList(java.util.List<T> l) {
        return fromIterator(l.iterator());
    }

    private static <T> List<T> fromIterator(Iterator<T> it) {
        return new List<T>(() -> Optional.of(0)
                .filter(u -> it.hasNext())
                .map(u -> new Pair<T>(it.next(), fromIterator(it))));
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

    Optional<T> max(BiFunction<T, T, Integer> cmp) {
        return fold(Optional.empty(), (acc, val) -> acc
                .filter(m -> cmp.apply(m, val) > 0)
                .or(() -> Optional.of(val)));
    }

    @Override
    public List<T> unique(List<T> hist, BiPredicate<? super T, ? super T> eq) {
        return new List<>(bind(opt -> opt.map(n -> n.unique(hist, eq)).orElse(List.nul())));
    }

    List<T> unique(BiPredicate<? super T, ? super T> eq) {
        return unique(List.nul(), eq);
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

    String strJoin(String delim) {
        return get().map(n -> n.next.fold(n.val.toString(),
                (acc, v) -> acc + delim + v))
                .orElse("");
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
    public List<T> unique(List<T> hist, BiPredicate<? super T, ? super T> eq) {
        return hist.find(v -> eq.test(v, val))
                .map(u -> next.unique(hist, eq))
                .orElseGet(() -> next.unique(hist.cons(val), eq).cons(val));
    }
}

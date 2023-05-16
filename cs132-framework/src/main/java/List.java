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

    Lazy(Function<Lazy<T>, T> f) {
        this.s = () -> f.apply(this);
    }

    @Override
    public T get() {
        final var v = s.get();
        s = () -> v;
        return v;
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

    <U> boolean equals(List<U> other, BiFunction<T, U, Boolean> f);

    List<T> join(List<T> other);

    int count();

    Optional<Integer> firstIndex(int i, Predicate<? super T> p);

    Optional<T> head();

    List<T> unique(List<T> hist, BiPredicate<T, T> eq);
}

public class List<T> extends Lazy<ListElem<T>> implements ListInt<T> {
    List(Supplier<ListElem<T>> s) {
        super(s);
    }

    @Override
    public Optional<T> find(Predicate<? super T> p) {
        return get().find(p);
    }

    @Override
    public <U> boolean equals(List<U> other, BiFunction<T, U, Boolean> f) {
        return get().equals(other, f);
    }

    static <T> List<T> nul() {
        return new List<>(Null::new);
    }

    List<T> cons(T val) {
        return new List<>(() -> new Pair<>(val, this));
    }

    static <T> List<T> of(T val) {
        return List.<T>nul().cons(val);
    }

    boolean forAll(Predicate<T> p) {
        return !exists(p.negate());
    }

    boolean exists(Predicate<T> p) {
        return find(p).isPresent();
    }

    @Override
    public <U> List<U> map(Function<T, U> f) {
        return new List<>(bind(l -> l.map(f)));
    }

    @Override
    public <U> U fold(U acc, BiFunction<U, T, U> f) {
        return get().fold(acc, f);
    }

    java.util.List<T> toJavaList() {
        return fold(new java.util.ArrayList<>(), (l, v) -> {
            l.add(v);
            return l;
        });
    }

    @Override
    public <U> List<U> flatMap(Function<T, List<U>> f) {
        return new List<>(bind(l -> l.flatMap(f)));
    }

    @Override
    public List<T> join(List<T> other) {
        return new List<>(bind(l -> l.join(other)));
    }

    @Override
    public List<T> filter(Predicate<? super T> p) {
        return new List<>(bind(l -> l.filter(p)));
    }

    private final Lazy<Integer> count = new Lazy<>(() -> get().count());

    @Override
    public int count() {
        return count.get();
    }

    List<T> reverse() {
        return fold(List.nul(), List::cons);
    }

    @Override
    public Optional<Integer> firstIndex(int i, Predicate<? super T> p) {
        return get().firstIndex(i, p);
    }

    Optional<Integer> firstIndex(Predicate<? super T> p) {
        return firstIndex(0, p);
    }

    @Override
    public Optional<T> head() {
        return get().head();
    }

    @Override
    public List<T> unique(List<T> hist, BiPredicate<T, T> eq) {
        return new List<>(bind(l -> l.unique(hist, eq)));
    }

    List<T> unique(BiPredicate<T, T> eq) {
        return unique(List.nul(), eq);
    }
}

abstract class ListElem<T> implements ListInt<T> {
}

class Null<T> extends ListElem<T> {
    @Override
    public Optional<T> find(Predicate<? super T> p) {
        return Optional.empty();
    }

    @Override
    public <U> boolean equals(List<U> other, BiFunction<T, U, Boolean> f) {
        return other.get() instanceof Null<?>;
    }

    @Override
    public <U> List<U> map(Function<T, U> f) {
        return List.nul();
    }

    @Override
    public <U> U fold(U acc, BiFunction<U, T, U> f) {
        return acc;
    }

    @Override
    public <U> List<U> flatMap(Function<T, List<U>> f) {
        return List.nul();
    }

    @Override
    public List<T> join(List<T> other) {
        return other;
    }

    @Override
    public List<T> filter(Predicate<? super T> p) {
        return List.nul();
    }

    @Override
    public int count() {
        return 0;
    }

    @Override
    public Optional<Integer> firstIndex(int i, Predicate<? super T> p) {
        return Optional.empty();
    }

    @Override
    public Optional<T> head() {
        return Optional.empty();
    }

    @Override
    public List<T> unique(List<T> hist, BiPredicate<T, T> eq) {
        return List.nul();
    }
}

class Pair<T> extends ListElem<T> {
    final T val;
    final List<T> next;

    Pair(T val, Supplier<ListElem<T>> next) {
        this.val = val;
        this.next = new List<>(next);
    }

    @Override
    public Optional<T> find(Predicate<? super T> p) {
        return Optional.of(val).filter(p).or(() -> next.find(p));
    }

    @Override
    public <U> boolean equals(List<U> other, BiFunction<T, U, Boolean> f) {
        return Optional.of(other.get())
                .filter(o -> o instanceof Pair<?>)
                .map(o -> (Pair<U>) o)
                .map(p -> f.apply(val, p.val) && next.equals(p.next, f))
                .orElse(false);
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
    public List<T> join(List<T> other) {
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
    public Optional<T> head() {
        return Optional.of(val);
    }

    @Override
    public List<T> unique(List<T> hist, BiPredicate<T, T> eq) {
        if (hist.exists(v -> eq.test(v, val)))
            return next.unique(hist, eq);
        else
            return next.unique(hist.cons(val), eq).cons(val);
    }
}

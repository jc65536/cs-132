import java.util.*;
import java.util.function.*;

class Lazy<T> implements Supplier<T> {
    T v;
    Supplier<T> s;

    Lazy(Supplier<T> s) {
        this.s = s;
    }

    Lazy(Function<Lazy<T>, T> f) {
        this.s = () -> f.apply(this);
    }

    @Override
    public T get() {
        if (s != null) {
            v = s.get();
            s = null;
        }

        return v;
    }

    <U> Lazy<U> bind(Function<T, Lazy<U>> f) {
        return new Lazy<>(() -> f.apply(get()).get());
    }
}

interface ListInt<T> {
    Optional<T> find(Predicate<? super T> p);

    <U> U fold(U acc, BiFunction<U, T, U> f);

    List<T> join(List<T> other);

    <U> boolean equals(List<U> other, BiFunction<T, U, Boolean> f);
}

public class List<T> extends Lazy<_List<T>> implements ListInt<T> {
    List(Supplier<_List<T>> s) {
        super(s);
    }

    @Override
    public Optional<T> find(Predicate<? super T> p) {
        return get().find(p);
    }

    @Override
    public <U> U fold(U acc, BiFunction<U, T, U> f) {
        return get().fold(acc, f);
    }

    @Override
    public List<T> join(List<T> other) {
        return new List<>(bind(l -> l.join(other)));
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

    boolean exists(Predicate<? super T> p) {
        return find(p).isPresent();
    }
}

abstract class _List<T> implements ListInt<T> {
}

class Null<T> extends _List<T> {
    @Override
    public Optional<T> find(Predicate<? super T> p) {
        return Optional.empty();
    }

    @Override
    public <U> U fold(U acc, BiFunction<U, T, U> f) {
        return acc;
    }

    @Override
    public List<T> join(List<T> other) {
        return other;
    }

    @Override
    public <U> boolean equals(List<U> other, BiFunction<T, U, Boolean> f) {
        return other.get() instanceof Null<?>;
    }
}

class Pair<T> extends _List<T> {
    final T val;
    final List<T> next;

    Pair(T val, Supplier<_List<T>> next) {
        this.val = val;
        this.next = new List<>(next);
    }

    @Override
    public Optional<T> find(Predicate<? super T> p) {
        // System.out.println("find: " + val);
        if (p.test(val))
            return Optional.of(val);
        else
            return next.find(p);
    }

    @Override
    public <U> U fold(U acc, BiFunction<U, T, U> f) {
        return next.fold(f.apply(acc, val), f);
    }

    @Override
    public List<T> join(List<T> other) {
        // System.out.println("join: " + val);
        return next.join(other).cons(val);
    }

    @Override
    public <U> boolean equals(List<U> other, BiFunction<T, U, Boolean> f) {
        final var _other = other.get();

        if (!(_other instanceof Pair<?>))
            return false;

        final var o = (Pair<U>) _other;

        return f.apply(val, o.val) && next.equals(o.next, f);
    }
}

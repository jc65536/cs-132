import java.util.*;
import java.util.function.*;

interface ListInt<T> {
    Optional<T> find(Predicate<T> p);

    <U> U fold(U acc, BiFunction<U, T, U> f);

    List<T> join(List<T> other);

    <U> List<U> map(Function<T, U> f);

    <U> List<U> flatMap(Function<T, List<U>> f);

    <U> boolean equals(List<U> other, BiFunction<T, U, Boolean> f);
}

public class List<T> extends Lazy<_List<T>> implements ListInt<T> {
    List(Supplier<_List<T>> s) {
        super(s);
    }

    @Override
    public Optional<T> find(Predicate<T> p) {
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
    public <U> List<U> map(Function<T, U> f) {
        return new List<>(bind(l -> l.map(f)));
    }

    @Override
    public <U> List<U> flatMap(Function<T, List<U>> f) {
        return new List<>(bind(l -> l.flatMap(f)));
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
}

abstract class _List<T> implements ListInt<T> {
}

class Null<T> extends _List<T> {
    @Override
    public Optional<T> find(Predicate<T> p) {
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
    public <U> List<U> map(Function<T, U> f) {
        return List.nul();
    }

    @Override
    public <U> List<U> flatMap(Function<T, List<U>> f) {
        return List.nul();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Null<?>;
    }

    @Override
    public <U> boolean equals(List<U> other, BiFunction<T, U, Boolean> f) {
        return equals(other.get());
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
    public Optional<T> find(Predicate<T> p) {
        // System.out.println("find: " + val);
        if (p.test(val))
            return Optional.of(val);
        else
            return next.get().find(p);
    }

    @Override
    public <U> U fold(U acc, BiFunction<U, T, U> f) {
        return next.get().fold(f.apply(acc, val), f);
    }

    @Override
    public List<T> join(List<T> other) {
        // System.out.println("join: " + val);
        return new List<>(() -> new Pair<T>(val, next.join(other)));
    }

    @Override
    public <U> List<U> map(Function<T, U> f) {
        // System.out.println("map: " + val);
        return new List<>(() -> new Pair<>(f.apply(val), next.map(f)));
    }

    @Override
    public <U> List<U> flatMap(Function<T, List<U>> f) {
        // System.out.println("flatmap: " + val);
        return new List<>(() -> f.apply(val).get().join(next.flatMap(f)).get());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Pair<?> && equals(new List<>(() -> (Pair<?>) obj), (u, v) -> u.equals(v));
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

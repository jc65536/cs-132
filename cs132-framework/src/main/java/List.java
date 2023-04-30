import java.util.*;
import java.util.function.*;

abstract class List<T> {
    abstract Optional<T> find(Predicate<T> p);

    abstract <U> U fold(U acc, BiFunction<U, T, U> f);

    abstract Lazy<List<T>> join(Lazy<List<T>> other);

    abstract <U> Lazy<List<U>> map(Function<T, U> f);

    abstract <U> Lazy<List<U>> flatMap(Function<T, Lazy<List<U>>> f);

    abstract <U> boolean equals(List<U> other, BiFunction<T, U, Boolean> f);

    static <T> List<T> nul() {
        return new Null<>();
    }

    List<T> cons(T val) {
        return new Pair<>(val, () -> this);
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

class Null<T> extends List<T> {
    @Override
    Optional<T> find(Predicate<T> p) {
        return Optional.empty();
    }

    @Override
    <U> U fold(U acc, BiFunction<U, T, U> f) {
        return acc;
    }

    @Override
    Lazy<List<T>> join(Lazy<List<T>> other) {
        return other;
    }

    @Override
    <U> Lazy<List<U>> map(Function<T, U> f) {
        return new Lazy<>(List::nul);
    }

    @Override
    <U> Lazy<List<U>> flatMap(Function<T, Lazy<List<U>>> f) {
        return new Lazy<>(List::nul);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Null<?>;
    }

    @Override
    <U> boolean equals(List<U> other, BiFunction<T, U, Boolean> f) {
        return equals(other);
    }
}

class Pair<T> extends List<T> {
    final T val;
    final Lazy<List<T>> next;

    Pair(T val, Supplier<List<T>> next) {
        this.val = val;
        this.next = new Lazy<>(next);
    }

    @Override
    Optional<T> find(Predicate<T> p) {
        // System.out.println("find: " + val);
        if (p.test(val))
            return Optional.of(val);
        else
            return next.get().find(p);
    }

    @Override
    <U> U fold(U acc, BiFunction<U, T, U> f) {
        return next.get().fold(f.apply(acc, val), f);
    }

    @Override
    Lazy<List<T>> join(Lazy<List<T>> other) {
        // Syst:m.out.println("join: " + val);
        return new Lazy<>(() -> new Pair<T>(val, next.flatMap(n -> n.join(other))));
    }

    @Override
    <U> Lazy<List<U>> map(Function<T, U> f) {
        // System.out.println("map: " + val);
        return new Lazy<>(() -> new Pair<>(f.apply(val), next.flatMap(n -> n.map(f))));
    }

    @Override
    <U> Lazy<List<U>> flatMap(Function<T, Lazy<List<U>>> f) {
        // System.out.println("flatmap: " + val);
        return new Lazy<>(() -> f.apply(val).get().join(next.flatMap(n -> n.flatMap(f))).get());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Pair<?> && equals((Pair<?>) obj, (u, v) -> u.equals(v));
    }

    @Override
    <U> boolean equals(List<U> other, BiFunction<T, U, Boolean> f) {
        if (!(other instanceof Pair<?>))
            return false;

        final var o = (Pair<U>) other;

        return f.apply(val, o.val) && next.get().equals(o.next.get(), f);
    }
}

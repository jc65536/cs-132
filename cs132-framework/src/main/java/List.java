import java.util.*;
import java.util.function.*;

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

    <U> Lazy<U> bind(Function<T, Lazy<U>> f) {
        return new Lazy<>(() -> f.apply(get()).get());
    }
}

interface ListInt<T> {
    Optional<T> find(Predicate<? super T> p);

    <U> U fold(U acc, BiFunction<U, T, U> f);

    List<T> join(List<T> other);

    <U> List<U> map(Function<T, U> f);

    <U> boolean equals(List<U> other, BiFunction<T, U, Boolean> f);

    <U> Optional<List<U>> failMap(Function<T, Optional<? extends U>> f);

    Optional<List<T>> forceDistinct(BiPredicate<List<T>, T> p);
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

    boolean forAll(Predicate<T> p) {
        return !exists(p.negate());
    }

    boolean exists(Predicate<? super T> p) {
        return find(p).isPresent();
    }

    @Override
    public <U> List<U> map(Function<T, U> f) {
        return new List<>(bind(l -> l.map(f)));
    }

    @Override
    public <U> Optional<List<U>> failMap(Function<T, Optional<? extends U>> f) {
        return get().failMap(f);
    }

    @Override
    public Optional<List<T>> forceDistinct(BiPredicate<List<T>, T> p) {
        return get().forceDistinct(p);
    }
}

interface ListElem<T> extends ListInt<T> {
}

class Null<T> implements ListElem<T> {
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

    @Override
    public <U> List<U> map(Function<T, U> f) {
        return List.nul();
    }

    @Override
    public <U> Optional<List<U>> failMap(Function<T, Optional<? extends U>> f) {
        return Optional.of(List.nul());
    }

    @Override
    public Optional<List<T>> forceDistinct(BiPredicate<List<T>, T> p) {
        return Optional.of(List.nul());
    }
}

class Pair<T> implements ListElem<T> {
    final T val;
    final List<T> next;

    Pair(T val, Supplier<ListElem<T>> next) {
        this.val = val;
        this.next = new List<>(next);
    }

    @Override
    public Optional<T> find(Predicate<? super T> p) {
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

    @Override
    public <U> List<U> map(Function<T, U> f) {
        return next.map(f).cons(f.apply(val));
    }

    @Override
    public <U> Optional<List<U>> failMap(Function<T, Optional<? extends U>> f) {
        return f.apply(val).flatMap(u -> next.failMap(f).map(list -> list.cons(u)));
    }

    @Override
    public Optional<List<T>> forceDistinct(BiPredicate<List<T>, T> p) {
        return next.forceDistinct(p)
                .filter(list -> p.test(list, val))
                .map(list -> list.cons(val));
    }
}

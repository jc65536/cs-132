import java.util.*;
import java.util.function.*;

public abstract class List<T> {
    abstract Optional<T> find(Predicate<T> p);

    abstract <U> U fold(U acc, BiFunction<U, T, U> f);

    abstract <U> boolean equals(List<U> other, BiFunction<T, U, Boolean> f);

    private static class Null<T> extends List<T> {
        @Override
        public Optional<T> find(Predicate<T> p) {
            return Optional.empty();
        }

        @Override
        <U> U fold(U acc, BiFunction<U, T, U> f) {
            return acc;
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

    private static class Node<T> extends List<T> {
        final T val;
        final List<T> next;

        Node(T val, List<T> next) {
            this.val = val;
            this.next = next;
        }

        @Override
        public Optional<T> find(Predicate<T> p) {
            if (p.test(val))
                return Optional.of(val);
            else
                return next.find(p);
        }

        @Override
        <U> U fold(U acc, BiFunction<U, T, U> f) {
            return next.fold(f.apply(acc, val), f);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Node<?> && equals((Node<?>) obj, (u, v) -> u.equals(v));
        }

        @Override
        <U> boolean equals(List<U> other, BiFunction<T, U, Boolean> f) {
            if (!(other instanceof Node<?>))
                return false;

            final var o = (Node<U>) other;

            return f.apply(val, o.val) && next.equals(o.next, f);
        }
    }

    static <T> List<T> nul() {
        return new Null<>();
    }

    List<T> cons(T val) {
        return new Node<T>(val, this);
    }

    static <T> List<T> of(T val) {
        return List.<T>nul().cons(val);
    }

    List<T> join(List<T> other) {
        return other.fold(this, (list, val) -> list.cons(val));
    }

    boolean forall(Predicate<T> p) {
        return !exists(p.negate());
    }

    boolean exists(Predicate<T> p) {
        return this.find(p).isPresent();
    }
}

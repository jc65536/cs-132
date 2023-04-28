import java.util.*;
import java.util.function.*;

class Lazy<T> implements Supplier<T> {
    private T v;
    private Supplier<T> f;

    Lazy(Supplier<T> f) {
        this.f = f;
    }

    Lazy(Function<Lazy<T>, T> f) {
        this.f = () -> f.apply(this);
    }

    @Override
    public T get() {
        if (f != null) {
            v = f.get();
            f = null;
        }

        return v;
    }
}

class List<T> {
    final T val;
    final List<T> next;

    List(T val, List<T> next) {
        this.val = val;
        this.next = next;
    }

    Optional<T> find(Predicate<T> p) {
        if (p.test(val))
            return Optional.of(val);
        else if (next == null)
            return Optional.empty();
        else
            return next.find(p);
    }

    void foreach(Consumer<T> c) {
        c.accept(val);
        if (next != null)
            next.foreach(c);
    }

    boolean equals(List<T> other) {
        return val.equals(other.val) && next.equals(other.next);
    }
}
